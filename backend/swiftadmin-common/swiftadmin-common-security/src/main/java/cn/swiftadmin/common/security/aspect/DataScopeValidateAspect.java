package cn.swiftadmin.common.security.aspect;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.mybatis.annotation.DataScopeValidate;
import cn.swiftadmin.common.mybatis.service.DataScopeValidationService;
import cn.swiftadmin.common.mybatis.service.UserInfoQueryService;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限校验切面
 * <p>处理 @DataScopeValidate 注解，在 UPDATE/DELETE 操作前进行数据权限校验</p>
 * <p>支持：单个用户ID、用户ID列表(List&lt;Long&gt;)、DTO对象</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Aspect
@Component
public class DataScopeValidateAspect {

    private final DataScopeValidationService validationService;

    @Autowired(required = false)
    private UserInfoQueryService userInfoQueryService;

    public DataScopeValidateAspect(DataScopeValidationService validationService) {
        this.validationService = validationService;
    }

    private static final String[] USER_ID_PARAMS = {"userId", "id", "targetUserId"};
    private static final String[] USER_IDS_PARAMS = {"userIds", "ids", "targetUserIds"};
    private static final String[] DEPT_ID_PARAMS = {"deptId", "departmentId", "targetDeptId"};

    @Around("@annotation(cn.swiftadmin.common.mybatis.annotation.DataScopeValidate)")
    public Object validateDataScope(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataScopeValidate annotation = method.getAnnotation(DataScopeValidate.class);

        if (annotation.mode() == DataScopeValidate.ValidateMode.SKIP) {
            return joinPoint.proceed();
        }

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null && loginUser.isSuperAdmin()) {
            return joinPoint.proceed();
        }

        // 尝试提取用户ID列表（批量操作）
        List<Long> userIds = extractUserIdList(joinPoint, method);

        if (!userIds.isEmpty()) {
            return handleBatchValidation(joinPoint, annotation, userIds);
        }

        // 单个用户校验
        Long targetUserId = extractParameter(joinPoint, method, USER_ID_PARAMS, Long.class);
        Long targetDeptId = extractParameter(joinPoint, method, DEPT_ID_PARAMS, Long.class);

        if (targetUserId == null && targetDeptId == null) {
            Object dto = findDtoParameter(joinPoint, method);
            if (dto != null) {
                if (targetUserId == null) {
                    targetUserId = extractField(dto, USER_ID_PARAMS);
                }
                if (targetDeptId == null) {
                    targetDeptId = extractField(dto, DEPT_ID_PARAMS);
                }
            }
        }

        // 自动查询用户部门
        if (targetUserId != null && targetDeptId == null && annotation.autoQueryDept()) {
            targetDeptId = queryUserDept(targetUserId);
        }

        boolean hasPermission = validationService.validateDataScope(targetUserId, targetDeptId);

        if (!hasPermission) {
            log.warn("数据权限校验失败：userId={}, targetUserId={}, targetDeptId={}",
                    SecurityUtils.getCurrentUserId(), targetUserId, targetDeptId);

            if (annotation.mode() == DataScopeValidate.ValidateMode.THROW) {
                throw new BusinessException(403, annotation.message());
            }
            return false;
        }

        return joinPoint.proceed();
    }

    /**
     * 处理批量校验
     */
    private Object handleBatchValidation(ProceedingJoinPoint joinPoint,
                                          DataScopeValidate annotation,
                                          List<Long> userIds) throws Throwable {
        Set<Long> permittedUserIds = new HashSet<>();
        Set<Long> deniedUserIds = new HashSet<>();

        // 批量查询用户部门，避免 N+1 问题
        Map<Long, Long> userDeptMap = batchQueryUserDepts(userIds);

        for (Long userId : userIds) {
            Long deptId = userDeptMap.getOrDefault(userId, null);
            boolean hasPermission = validationService.validateDataScope(userId, deptId);

            if (hasPermission) {
                permittedUserIds.add(userId);
            } else {
                deniedUserIds.add(userId);
            }
        }

        // 根据批量模式处理
        switch (annotation.batchMode()) {
            case ALL_OR_NONE -> {
                if (!deniedUserIds.isEmpty()) {
                    log.warn("批量数据权限校验失败：userId={}, deniedUserIds={}",
                            SecurityUtils.getCurrentUserId(), deniedUserIds);
                    if (annotation.mode() == DataScopeValidate.ValidateMode.THROW) {
                        throw new BusinessException(403,
                                String.format("无权限操作 %d 个用户的数据", deniedUserIds.size()));
                    }
                    return false;
                }
                return joinPoint.proceed();
            }
            case FILTER -> {
                // 过滤模式：修改参数为只包含有权限的用户ID
                if (permittedUserIds.isEmpty()) {
                    log.warn("批量数据权限校验：全部被拒绝");
                    return null;
                }
                // 注入过滤后的用户ID列表
                injectFilteredUserIds(joinPoint, permittedUserIds);
                log.info("批量数据权限过滤：total={}, permitted={}, denied={}",
                        userIds.size(), permittedUserIds.size(), deniedUserIds.size());
                return joinPoint.proceed();
            }
            case ANY -> {
                if (permittedUserIds.isEmpty()) {
                    log.warn("批量数据权限校验失败：无任何权限");
                    if (annotation.mode() == DataScopeValidate.ValidateMode.THROW) {
                        throw new BusinessException(403, "无权限操作任何数据");
                    }
                    return false;
                }
                // 注入过滤后的用户ID列表
                injectFilteredUserIds(joinPoint, permittedUserIds);
                return joinPoint.proceed();
            }
            default -> {
                return joinPoint.proceed();
            }
        }
    }

    /**
     * 批量查询用户部门
     * <p>避免 N+1 查询问题</p>
     */
    private Map<Long, Long> batchQueryUserDepts(List<Long> userIds) {
        if (userInfoQueryService == null) {
            log.warn("UserInfoQueryService 未注入，无法批量查询用户部门");
            return Collections.emptyMap();
        }
        try {
            return userInfoQueryService.getDeptIdsByUserIds(userIds);
        } catch (Exception e) {
            log.warn("批量查询用户部门失败: userIds={}", userIds, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 提取用户ID列表参数
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractUserIdList(ProceedingJoinPoint joinPoint, Method method) {
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (String paramName : USER_IDS_PARAMS) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getName().equals(paramName) && args[i] != null) {
                    if (args[i] instanceof List) {
                        try {
                            List<?> list = (List<?>) args[i];
                            if (!list.isEmpty() && list.get(0) instanceof Long) {
                                return (List<Long>) list;
                            }
                            // 处理 Integer 类型的 ID
                            if (!list.isEmpty() && list.get(0) instanceof Integer) {
                                return list.stream()
                                        .map(item -> ((Number) item).longValue())
                                        .collect(Collectors.toList());
                            }
                        } catch (ClassCastException e) {
                            log.debug("参数 {} 类型转换失败", paramName, e);
                        }
                    }
                }
            }
        }

        // 尝试从 DTO 中提取用户ID列表
        Object dto = findDtoParameter(joinPoint, method);
        if (dto != null) {
            Object fieldValue = extractFieldValue(dto, USER_IDS_PARAMS);
            if (fieldValue instanceof List) {
                try {
                    List<?> list = (List<?>) fieldValue;
                    if (!list.isEmpty()) {
                        return list.stream()
                                .map(item -> {
                                    if (item instanceof Long) return (Long) item;
                                    if (item instanceof Number) return ((Number) item).longValue();
                                    return null;
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    }
                } catch (Exception e) {
                    log.debug("从 DTO 提取用户ID列表失败", e);
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * 注入过滤后的用户ID列表
     * <p>通过修改 List 参数内容来实现参数注入</p>
     */
    @SuppressWarnings("unchecked")
    private void injectFilteredUserIds(ProceedingJoinPoint joinPoint, Set<Long> permittedUserIds) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        List<Long> filteredList = new ArrayList<>(permittedUserIds);

        // 查找并修改 List<Long> 类型的参数
        for (String paramName : USER_IDS_PARAMS) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getName().equals(paramName) && args[i] instanceof List) {
                    try {
                        List<Object> list = (List<Object>) args[i];
                        // 清空原列表并添加过滤后的ID
                        list.clear();
                        list.addAll(filteredList);
                        log.debug("注入过滤后的用户ID列表: {} -> {}", paramName, filteredList);
                        return;
                    } catch (UnsupportedOperationException e) {
                        // 不可修改的列表（如 Collections.unmodifiableList）
                        log.warn("参数 {} 是不可修改的列表，无法注入过滤后的用户ID", paramName);
                    } catch (ClassCastException e) {
                        log.debug("参数 {} 类型转换失败", paramName);
                    }
                }
            }
        }

        // 尝试修改 DTO 中的字段
        Object dto = findDtoParameter(joinPoint, method);
        if (dto != null) {
            injectFilteredUserIdsToDto(dto, filteredList);
        }
    }

    /**
     * 将过滤后的用户ID注入到 DTO 对象中
     */
    private void injectFilteredUserIdsToDto(Object dto, List<Long> filteredList) {
        for (String fieldName : USER_IDS_PARAMS) {
            try {
                Field field = findField(dto.getClass(), fieldName);
                if (field != null && List.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) field.get(dto);
                    if (list != null) {
                        list.clear();
                        list.addAll(filteredList);
                        log.debug("注入过滤后的用户ID列表到 DTO 字段: {} -> {}", fieldName, filteredList);
                        return;
                    }
                }
            } catch (Exception e) {
                log.debug("注入 DTO 字段 {} 失败", fieldName, e);
            }
        }
    }

    /**
     * 查询用户部门
     */
    private Long queryUserDept(Long userId) {
        if (userInfoQueryService == null) {
            log.warn("UserInfoQueryService 未注入，无法自动查询用户部门");
            return null;
        }
        try {
            return userInfoQueryService.getDeptIdByUserId(userId);
        } catch (Exception e) {
            log.warn("查询用户部门失败: userId={}", userId, e);
            return null;
        }
    }

    private <T> T extractParameter(ProceedingJoinPoint joinPoint, Method method, String[] paramNames, Class<T> type) {
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (String paramName : paramNames) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getName().equals(paramName) && args[i] != null) {
                    try {
                        return type.cast(args[i]);
                    } catch (ClassCastException e) {
                        log.debug("参数 {} 类型转换失败，期望类型: {}", paramName, type.getName(), e);
                    }
                }
            }
        }
        return null;
    }

    private Object findDtoParameter(ProceedingJoinPoint joinPoint, Method method) {
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (args[i] != null && !isSimpleType(args[i].getClass())) {
                return args[i];
            }
        }
        return null;
    }

    private Long extractField(Object obj, String[] fieldNames) {
        Object value = extractFieldValue(obj, fieldNames);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Object extractFieldValue(Object obj, String[] fieldNames) {
        Class<?> clazz = obj.getClass();
        for (String fieldName : fieldNames) {
            try {
                Field field = findField(clazz, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value != null) {
                        return value;
                    }
                }
            } catch (Exception e) {
                log.debug("提取字段 {} 值失败", fieldName, e);
            }
        }

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (String fieldName : fieldNames) {
                Object value = map.get(fieldName);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findField(superClass, fieldName);
            }
            log.debug("未找到字段: {} in {}", fieldName, clazz.getName());
            return null;
        }
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                Number.class.isAssignableFrom(clazz) ||
                clazz == Boolean.class ||
                clazz.isEnum() ||
                List.class.isAssignableFrom(clazz);
    }
}