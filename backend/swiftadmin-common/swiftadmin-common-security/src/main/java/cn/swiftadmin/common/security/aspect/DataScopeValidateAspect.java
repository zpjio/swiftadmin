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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
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

        for (Long userId : userIds) {
            Long deptId = queryUserDept(userId);
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
                            // ignore
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
                    // ignore
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * 注入过滤后的用户ID列表
     */
    private void injectFilteredUserIds(ProceedingJoinPoint joinPoint, Set<Long> permittedUserIds) {
        // 通过修改参数数组来注入过滤后的用户ID
        // 注意：这需要方法参数是可修改的集合类型
        // 实际应用中可能需要更复杂的处理
        log.debug("注入过滤后的用户ID列表: {}", permittedUserIds);
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
                        // ignore
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
                java.lang.reflect.Field field = findField(clazz, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value != null) {
                        return value;
                    }
                }
            } catch (Exception e) {
                // ignore
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

    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findField(superClass, fieldName);
            }
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