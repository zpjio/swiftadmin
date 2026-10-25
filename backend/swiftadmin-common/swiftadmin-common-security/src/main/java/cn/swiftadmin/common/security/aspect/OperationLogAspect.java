package cn.swiftadmin.common.security.aspect;

import cn.swiftadmin.common.security.annotation.AuditIgnore;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.event.OperationLogEvent;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.security.util.SecurityUtils;
import tools.jackson.databind.ObjectMapper;
import com.mybatisflex.annotation.Table;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 操作日志切面
 * <p>自动记录标注 @OperationLog 的方法操作日志</p>
 * <p>支持记录数据变更（审计功能）</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final DataSource dataSource;

    /** 字段缓存，避免重复反射 */
    private final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    public OperationLogAspect(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper, DataSource dataSource) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
    }

    @Around("@annotation(cn.swiftadmin.common.security.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperationLog operationLog = signature.getMethod().getAnnotation(OperationLog.class);

        // 初始化日志数据
        Map<String, Object> logData = new LinkedHashMap<>();

        // 基本信息
        logData.put("title", operationLog.title());
        logData.put("businessType", operationLog.businessType().getCode());
        logData.put("operatorType", operationLog.operatorType().getCode());
        logData.put("operTime", LocalDateTime.now());
        logData.put("method", joinPoint.getTarget().getClass().getName() + "." + signature.getName());

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logData.put("operUrl", request.getRequestURI());
            logData.put("operIp", ServletUtils.getClientIp(request));
            logData.put("requestMethod", request.getMethod());

            // 保存请求参数
            if (operationLog.isSaveRequestData()) {
                String params = getRequestParams(joinPoint, operationLog.excludeParamNames());
                logData.put("operParam", truncate(params, 2000));
            }
        }

        // 获取当前用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            logData.put("operUserId", loginUser.getUserId());
            logData.put("operName", loginUser.getUsername());
            logData.put("deptName", loginUser.getNickname());
        }

        // 获取表名（审计功能）
        Class<?> entityClass = operationLog.entityClass();
        if (entityClass != void.class) {
            String tableName = getTableName(entityClass);
            logData.put("tableName", tableName);
        }

        // 记录变更前数据（审计功能）- 适用于更新和删除操作
        Object beforeData = null;
        if (operationLog.isSaveBeforeData() && entityClass != void.class) {
            Object idValue = extractIdFromArgs(joinPoint);
            if (idValue != null) {
                if (isBatchIds(idValue)) {
                    // 批量删除：记录 ID 列表和被删除的数据摘要
                    List<Object> ids = toIdList(idValue);
                    logData.put("recordId", ids.toString());
                    // 查询并记录被删除数据的名称列表
                    List<String> names = new ArrayList<>();
                    for (Object id : ids) {
                        Object data = queryById(getTableName(entityClass), id, entityClass);
                        if (data != null) {
                            String name = extractNameFromData(data);
                            if (name != null) {
                                names.add(name);
                            }
                        }
                    }
                    if (!names.isEmpty()) {
                        logData.put("recordName", String.join(", ", names));
                    }
                } else {
                    // 单个操作
                    beforeData = queryById(getTableName(entityClass), idValue, entityClass);
                    if (beforeData != null) {
                        logData.put("beforeData", toJson(beforeData, operationLog.ignoreFields()));
                        // 提取记录名称
                        String recordName = extractNameFromData(beforeData);
                        if (recordName != null) {
                            logData.put("recordName", recordName);
                        }
                    }
                }
            }
        }

        // 执行方法
        Object result = null;
        Throwable throwable = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            // 计算耗时
            logData.put("costTime", System.currentTimeMillis() - startTime);

            // 设置状态
            if (throwable != null) {
                logData.put("status", 1);
                logData.put("errorMsg", truncate(getExceptionMessage(throwable), 2000));
            } else {
                logData.put("status", 0);

                // 保存响应结果
                if (operationLog.isSaveResponseData() && result != null) {
                    try {
                        logData.put("jsonResult", truncate(objectMapper.writeValueAsString(result), 2000));
                    } catch (Exception e) {
                        log.debug("序列化响应结果失败", e);
                    }
                }

                // 记录变更后数据（审计功能）- 适用于新增和更新操作
                if (operationLog.isSaveAfterData() && result != null) {
                    Object afterData = extractAfterData(result, entityClass);
                    if (afterData != null) {
                        logData.put("afterData", toJson(afterData, operationLog.ignoreFields()));

                        // 提取记录ID
                        Object recordId = extractIdFromData(afterData);
                        if (recordId != null) {
                            logData.put("recordId", recordId.toString());
                        }

                        // 如果变更前没有提取到名称，尝试从变更后数据提取
                        if (!logData.containsKey("recordName")) {
                            String recordName = extractNameFromData(afterData);
                            if (recordName != null) {
                                logData.put("recordName", recordName);
                            }
                        }
                    }
                }

                // 计算变更字段
                if (beforeData != null && logData.containsKey("afterData")) {
                    String afterJson = (String) logData.get("afterData");
                    String changedFields = computeChangedFields(beforeData, afterJson, operationLog.ignoreFields());
                    if (changedFields != null) {
                        logData.put("changedFields", changedFields);
                    }
                }
            }

            // 发布事件
            try {
                eventPublisher.publishEvent(new OperationLogEvent(logData));
            } catch (Exception e) {
                log.warn("发布操作日志事件失败", e);
            }
        }

        return result;
    }

    /**
     * 提取变更后数据
     * <p>处理 R<T> 包装类，提取实际数据</p>
     */
    private Object extractAfterData(Object result, Class<?> entityClass) {
        if (result == null) {
            return null;
        }

        // 处理基本类型（ID 返回值）
        if (result instanceof Long || result instanceof Integer || result instanceof String) {
            return null;
        }

        // 处理 R<T> 包装类
        if (result.getClass().getSimpleName().equals("R")) {
            try {
                Method getDataMethod = result.getClass().getMethod("getData");
                Object data = getDataMethod.invoke(result);
                if (data != null && (entityClass == void.class || entityClass.isInstance(data))) {
                    return data;
                }
                return data;
            } catch (Exception e) {
                log.debug("从 R<T> 中提取数据失败", e);
            }
        }

        return result;
    }

    /**
     * 从方法参数中提取 ID（支持单个 ID 和批量 IDs）
     *
     * @return 单个 ID 或 ID 列表
     */
    private Object extractIdFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

        if (args == null || paramNames == null) {
            return null;
        }

        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i];
            // 单个 ID 参数
            if ("id".equals(paramName) || paramName.endsWith("Id")) {
                return args[i];
            }
            // 批量 IDs 参数
            if ("ids".equals(paramName) || paramName.endsWith("Ids")) {
                return args[i];
            }
            // 从 DTO 中提取 id 字段
            Object id = extractIdFromDTO(args[i]);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    /**
     * 判断是否为批量 ID
     */
    private boolean isBatchIds(Object idValue) {
        return idValue instanceof List || idValue instanceof Object[];
    }

    /**
     * 将 ID 值转换为列表
     */
    @SuppressWarnings("unchecked")
    private List<Object> toIdList(Object idValue) {
        if (idValue instanceof List) {
            return (List<Object>) idValue;
        }
        if (idValue instanceof Object[]) {
            return Arrays.asList((Object[]) idValue);
        }
        return Collections.singletonList(idValue);
    }

    /**
     * 从 DTO 对象中提取 ID
     */
    private Object extractIdFromDTO(Object obj) {
        if (obj == null) {
            return null;
        }
        // 检查是否是 DTO 或 VO 对象
        String simpleName = obj.getClass().getSimpleName();
        if (simpleName.endsWith("DTO") || simpleName.endsWith("VO")) {
            return getFieldValue(obj, "id");
        }
        return null;
    }

    /**
     * 从数据对象中提取 ID
     */
    private Object extractIdFromData(Object data) {
        return getFieldValue(data, "id");
    }

    /**
     * 从数据对象中提取名称
     */
    private String extractNameFromData(Object data) {
        String[] nameFields = {"name", "title", "username", "nickname"};
        for (String field : nameFields) {
            Object value = getFieldValue(data, field);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * 根据 ID 从数据库查询数据
     */
    private Object queryById(String tableName, Object id, Class<?> entityClass) {
        // 验证表名，防止 SQL 注入
        if (!isValidTableName(tableName)) {
            log.warn("无效的表名: {}", tableName);
            return null;
        }

        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(rs, entityClass);
                }
            }
        } catch (Exception e) {
            log.debug("查询变更前数据失败: table={}, id={}", tableName, id, e);
        }
        return null;
    }

    /**
     * 验证表名是否合法
     */
    private boolean isValidTableName(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            return false;
        }
        // 只允许字母、数字、下划线
        return tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    /**
     * 将 ResultSet 映射到实体对象
     */
    private Object mapResultSetToEntity(ResultSet rs, Class<?> entityClass) throws Exception {
        Object entity = entityClass.getDeclaredConstructor().newInstance();
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            String fieldName = underscoreToCamel(columnName);
            Field field = getCachedField(entityClass, fieldName);

            if (field != null) {
                field.setAccessible(true);
                Object value = rs.getObject(i);
                if (value != null) {
                    value = convertValue(value, field.getType());
                }
                field.set(entity, value);
            }
        }
        return entity;
    }

    /**
     * 类型转换
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        // Timestamp -> LocalDateTime
        if (targetType == LocalDateTime.class && value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        // Long -> Integer
        if (targetType == Integer.class && value instanceof Long l) {
            return l.intValue();
        }
        // Integer -> Long
        if (targetType == Long.class && value instanceof Integer integer) {
            return integer.longValue();
        }
        return value;
    }

    /**
     * 获取缓存的字段
     */
    private Field getCachedField(Class<?> clazz, String fieldName) {
        List<Field> fields = fieldCache.computeIfAbsent(clazz, this::getAllFields);
        for (Field field : fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    /**
     * 获取表名
     */
    private String getTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && StringUtils.hasText(table.value())) {
            return table.value();
        }
        return camelToUnderscore(entityClass.getSimpleName());
    }

    /**
     * 转换为JSON（忽略指定字段）
     */
    private String toJson(Object obj, String[] ignoreFields) {
        try {
            if (obj == null) {
                return null;
            }
            Set<String> ignoreSet = new HashSet<>(Arrays.asList(ignoreFields));
            Map<String, Object> map = new LinkedHashMap<>();

            for (Field field : getCachedFields(obj.getClass())) {
                String fieldName = field.getName();
                if (ignoreSet.contains(fieldName) || field.isAnnotationPresent(AuditIgnore.class)) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(obj);
                map.put(fieldName, value);
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.debug("序列化对象失败", e);
            return null;
        }
    }

    /**
     * 计算变更字段
     */
    private String computeChangedFields(Object before, String afterJson, String[] ignoreFields) {
        try {
            String beforeJson = toJson(before, ignoreFields);
            if (beforeJson == null || afterJson == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> beforeMap = objectMapper.readValue(beforeJson, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> afterMap = objectMapper.readValue(afterJson, Map.class);

            List<Map<String, Object>> changes = new ArrayList<>();
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(beforeMap.keySet());
            allKeys.addAll(afterMap.keySet());

            for (String key : allKeys) {
                Object beforeValue = beforeMap.get(key);
                Object afterValue = afterMap.get(key);
                if (!Objects.equals(beforeValue, afterValue)) {
                    Map<String, Object> change = new LinkedHashMap<>();
                    change.put("field", key);
                    change.put("before", beforeValue);
                    change.put("after", afterValue);
                    changes.add(change);
                }
            }
            return changes.isEmpty() ? null : objectMapper.writeValueAsString(changes);
        } catch (Exception e) {
            log.debug("计算变更字段失败", e);
            return null;
        }
    }

    /**
     * 获取对象字段值
     */
    private Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) {
            return null;
        }
        try {
            for (Field field : getCachedFields(obj.getClass())) {
                if (field.getName().equals(fieldName)) {
                    field.setAccessible(true);
                    return field.get(obj);
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }

    /**
     * 获取缓存的字段列表
     */
    private List<Field> getCachedFields(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, this::getAllFields);
    }

    /**
     * 获取类所有字段（包括父类）
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(ProceedingJoinPoint joinPoint, String[] excludeParamNames) {
        try {
            String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (paramNames == null || args == null) {
                return "";
            }

            Set<String> excludeSet = new HashSet<>(Arrays.asList(excludeParamNames));
            Map<String, Object> params = new LinkedHashMap<>();

            for (int i = 0; i < paramNames.length; i++) {
                String paramName = paramNames[i];
                Object paramValue = args[i];

                if (excludeSet.contains(paramName)) {
                    continue;
                }

                if (paramValue instanceof HttpServletRequest
                        || paramValue instanceof HttpServletResponse
                        || paramValue instanceof MultipartFile) {
                    continue;
                }

                params.put(paramName, paramValue);
            }

            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.debug("获取请求参数失败", e);
            return "";
        }
    }

    /**
     * 下划线转驼峰
     */
    private String underscoreToCamel(String str) {
        if (str == null || !str.contains("_")) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }

    /**
     * 驼峰转下划线
     */
    private String camelToUnderscore(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    /**
     * 获取异常消息
     */
    private String getExceptionMessage(Throwable t) {
        String message = t.getMessage();
        if (!StringUtils.hasText(message)) {
            message = t.getClass().getSimpleName();
        }
        return message;
    }

}