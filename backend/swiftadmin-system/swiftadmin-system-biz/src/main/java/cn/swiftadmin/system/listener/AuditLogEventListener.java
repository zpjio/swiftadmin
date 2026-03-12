package cn.swiftadmin.system.listener;

import cn.swiftadmin.common.security.event.AuditLogEvent;
import cn.swiftadmin.system.api.entity.SysAuditLog;
import cn.swiftadmin.system.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审计日志事件监听器
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogEventListener {

    private final SysAuditLogMapper auditLogMapper;

    /**
     * 异步处理审计日志事件
     */
    @Async
    @EventListener
    public void handleAuditLogEvent(AuditLogEvent event) {
        try {
            Map<String, Object> logData = event.getLogData();

            SysAuditLog auditLog = SysAuditLog.builder()
                    .module(getString(logData, "module"))
                    .operation(getString(logData, "operation"))
                    .tableName(getString(logData, "tableName"))
                    .recordId(getString(logData, "recordId"))
                    .recordName(getString(logData, "recordName"))
                    .beforeData(getString(logData, "beforeData"))
                    .afterData(getString(logData, "afterData"))
                    .changedFields(getString(logData, "changedFields"))
                    .operUserId(getLong(logData, "operUserId"))
                    .operUserName(getString(logData, "operUserName"))
                    .operIp(getString(logData, "operIp"))
                    .operTime((LocalDateTime) logData.get("operTime"))
                    .build();

            auditLogMapper.insert(auditLog);
            log.debug("审计日志已保存: module={}, operation={}", auditLog.getModule(), auditLog.getOperation());
        } catch (Exception e) {
            log.error("保存审计日志失败", e);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return null;
    }

}