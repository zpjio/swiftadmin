package cn.swiftadmin.common.security.aspect;

import cn.swiftadmin.common.security.annotation.AuditIgnore;
import cn.swiftadmin.common.security.annotation.AuditLog;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.event.AuditLogEvent;
import cn.swiftadmin.common.security.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 审计日志切面
 * <p>自动记录标注 @AuditLog 的方法数据变更</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(cn.swiftadmin.common.security.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuditLog auditLog = signature.getMethod().getAnnotation(AuditLog.class);

        if (auditLog == null) {
            return joinPoint.proceed();
        }

        // 获取参数
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        // 记录变更前数据（UPDATE/DELETE）
        Object beforeData = null;
        if (auditLog.operation() == AuditLog.OperationType.UPDATE
                || auditLog.operation() == AuditLog.OperationType.DELETE) {
            beforeData = captureBeforeData(args, paramNames, auditLog);
        }

        // 执行方法
        Object result = joinPoint.proceed();

        // 记录审计日志
        try {
            recordAuditLog(joinPoint, auditLog, args, paramNames, beforeData, result);
        } catch (Exception e) {
            log.warn("记录审计日志失败", e);
        }

        return result;
    }

    /**
     * 捕获变更前数据
     */
    private Object captureBeforeData(Object[] args, String[] paramNames, AuditLog auditLog) {
        // 尝试从参数中获取ID，然后查询变更前数据
        // 这里需要根据具体业务实现，简化处理
        return null;
    }

    /**
     * 记录审计日志
     */
    private void recordAuditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog,
                                Object[] args, String[] paramNames,
                                Object beforeData, Object result) {
        try {
            Map<String, Object> logData = new HashMap<>();

            // 基本信息
            logData.put("module", auditLog.module());
            logData.put("operation", auditLog.operation().name());
            logData.put("operTime", LocalDateTime.now());

            // 表名
            String tableName = getTableName(auditLog.entityClass());
            logData.put("tableName", tableName);

            // 记录ID和名称
            String recordId = extractRecordId(args, paramNames, result, auditLog);
            String recordName = extractRecordName(args, paramNames, result, auditLog);

            logData.put("recordId", recordId);
            logData.put("recordName", recordName);

            // 变更数据
            if (auditLog.recordBeforeData() && beforeData != null) {
                logData.put("beforeData", toJson(beforeData, auditLog.ignoreFields()));
            }

            if (auditLog.recordAfterData()) {
                Object afterData = extractAfterData(result, auditLog);
                if (afterData != null) {
                    logData.put("afterData", toJson(afterData, auditLog.ignoreFields()));
                }
            }

            // 变更字段
            if (beforeData != null && logData.containsKey("afterData")) {
                String changedFields = computeChangedFields(beforeData, logData.get("afterData"), auditLog.ignoreFields());
                logData.put("changedFields", changedFields);
            }

            // 操作用户
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null) {
                logData.put("operUserId", loginUser.getUserId());
                logData.put("operUserName", loginUser.getUsername());
            }

            // 操作IP
            logData.put("operIp", getClientIp());

            // 发布事件
            eventPublisher.publishEvent(new AuditLogEvent(logData));

        } catch (Exception e) {
            log.error("构建审计日志失败", e);
        }
    }

    /**
     * 获取表名
     */
    @SuppressWarnings("unchecked")
    private String getTableName(Class<?> entityClass) {
        // 尝试通过反射获取 @Table 注解的 value
        try {
            Class<? extends java.lang.annotation.Annotation> tableAnnotationClass = 
                (Class<? extends java.lang.annotation.Annotation>) Class.forName("com.mybatisflex.annotation.Table");
            java.lang.annotation.Annotation annotation = entityClass.getAnnotation(tableAnnotationClass);
            if (annotation != null) {
                java.lang.reflect.Method valueMethod = annotation.getClass().getMethod("value");
                return (String) valueMethod.invoke(annotation);
            }
        } catch (Exception e) {
            // 忽略
        }
        // 默认使用类名转下划线
        return camelToUnderscore(entityClass.getSimpleName());
    }

    /**
     * 提取记录ID
     */
    private String extractRecordId(Object[] args, String[] paramNames, Object result, AuditLog auditLog) {
        // 优先从结果获取ID
        if (result != null) {
            Object id = getFieldValue(result, "id");
            if (id != null) {
                return id.toString();
            }
        }

        // 从参数获取ID
        if (args != null && paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if ("id".equals(paramNames[i]) || paramNames[i].endsWith("Id")) {
                    if (args[i] != null) {
                        return args[i].toString();
                    }
                }
                // 从DTO中获取ID
                if (args[i] != null && args[i].getClass().getSimpleName().endsWith("DTO")) {
                    Object id = getFieldValue(args[i], "id");
                    if (id != null) {
                        return id.toString();
                    }
                }
            }
        }

        return null;
    }

    /**
     * 提取记录名称
     */
    private String extractRecordName(Object[] args, String[] paramNames, Object result, AuditLog auditLog) {
        String spel = auditLog.recordName();
        if (spel == null || spel.isEmpty()) {
            return null;
        }

        // 简化处理：尝试从参数或结果中获取name/title字段
        String[] nameFields = {"name", "title", "username", "nickname"};
        for (String field : nameFields) {
            Object value = getFieldValue(result, field);
            if (value == null && args != null) {
                for (Object arg : args) {
                    value = getFieldValue(arg, field);
                    if (value != null) break;
                }
            }
            if (value != null) {
                return value.toString();
            }
        }

        return null;
    }

    /**
     * 提取变更后数据
     */
    private Object extractAfterData(Object result, AuditLog auditLog) {
        if (result == null) {
            return null;
        }
        // 如果返回的是ID，需要重新查询（简化处理直接返回null）
        if (result instanceof Long || result instanceof Integer || result instanceof String) {
            return null;
        }
        return result;
    }

    /**
     * 转换为JSON（忽略指定字段）
     */
    private String toJson(Object obj, String[] ignoreFields) {
        try {
            if (obj == null) {
                return null;
            }

            // 创建副本并忽略字段
            Map<String, Object> map = new LinkedHashMap<>();
            for (Field field : getAllFields(obj.getClass())) {
                field.setAccessible(true);
                String fieldName = field.getName();

                // 忽略指定字段
                if (Arrays.asList(ignoreFields).contains(fieldName)) {
                    continue;
                }

                // 忽略标注 @AuditIgnore 的字段
                if (field.isAnnotationPresent(AuditIgnore.class)) {
                    continue;
                }

                Object value = field.get(obj);
                map.put(fieldName, value);
            }

            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("序列化对象失败", e);
            return null;
        }
    }

    /**
     * 计算变更字段
     */
    private String computeChangedFields(Object before, Object after, String[] ignoreFields) {
        try {
            // 简化处理：比较两个对象的JSON
            Map<String, Object> beforeMap = objectMapper.readValue(toJson(before, ignoreFields), Map.class);
            Map<String, Object> afterMap = objectMapper.readValue(toJson(after, ignoreFields), Map.class);

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
            log.warn("计算变更字段失败", e);
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
            for (Field field : getAllFields(obj.getClass())) {
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
     * 获取客户端IP
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }
        return null;
    }

}