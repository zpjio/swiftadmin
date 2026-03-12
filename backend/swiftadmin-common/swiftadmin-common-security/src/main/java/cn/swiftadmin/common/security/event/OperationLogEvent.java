package cn.swiftadmin.common.security.event;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 操作日志事件
 * <p>用于解耦日志切面和日志服务</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public class OperationLogEvent extends ApplicationEvent {

    private final Map<String, Object> logData;

    public OperationLogEvent(Map<String, Object> logData) {
        super(logData);
        this.logData = logData;
    }

    public Map<String, Object> getLogData() {
        return logData;
    }

}