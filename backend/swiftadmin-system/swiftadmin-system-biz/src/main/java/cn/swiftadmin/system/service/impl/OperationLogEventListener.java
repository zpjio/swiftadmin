package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.security.event.OperationLogEvent;
import cn.swiftadmin.system.api.entity.SysOperLog;
import cn.swiftadmin.system.api.service.OperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志事件监听器
 * <p>异步处理操作日志事件，保存到数据库</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogEventListener {

    private final OperLogService operLogService;

    @Async
    @EventListener
    public void handleOperationLogEvent(OperationLogEvent event) {
        try {
            Map<String, Object> logData = event.getLogData();

            SysOperLog operLog = SysOperLog.builder()
                    .title(getString(logData, "title"))
                    .module(getString(logData, "module"))
                    .businessType(getInteger(logData, "businessType"))
                    .method(getString(logData, "method"))
                    .requestMethod(getString(logData, "requestMethod"))
                    .operatorType(getInteger(logData, "operatorType"))
                    .operUserId(getLong(logData, "operUserId"))
                    .operName(getString(logData, "operName"))
                    .deptName(getString(logData, "deptName"))
                    .operUrl(getString(logData, "operUrl"))
                    .operIp(getString(logData, "operIp"))
                    .operLocation(getString(logData, "operLocation"))
                    .operParam(getString(logData, "operParam"))
                    .jsonResult(getString(logData, "jsonResult"))
                    // 审计功能字段
                    .tableName(getString(logData, "tableName"))
                    .recordId(getString(logData, "recordId"))
                    .recordName(getString(logData, "recordName"))
                    .beforeData(getString(logData, "beforeData"))
                    .afterData(getString(logData, "afterData"))
                    .changedFields(getString(logData, "changedFields"))
                    // 执行信息
                    .status(getInteger(logData, "status"))
                    .errorMsg(getString(logData, "errorMsg"))
                    .operTime((LocalDateTime) logData.get("operTime"))
                    .costTime(getLong(logData, "costTime"))
                    .build();

            operLogService.save(operLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        return null;
    }

}