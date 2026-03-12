package cn.swiftadmin.common.security.event;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 审计日志事件
 * <p>用于解耦审计日志切面和日志服务</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public class AuditLogEvent extends ApplicationEvent {

    private final Map<String, Object> logData;

    public AuditLogEvent(Map<String, Object> logData) {
        super(logData);
        this.logData = logData;
    }

    public Map<String, Object> getLogData() {
        return logData;
    }

}