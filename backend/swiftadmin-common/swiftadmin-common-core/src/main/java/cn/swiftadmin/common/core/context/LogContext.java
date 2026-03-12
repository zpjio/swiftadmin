package cn.swiftadmin.common.core.context;

import cn.swiftadmin.common.core.enums.LogType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志上下文
 * 使用 ThreadLocal 存储当前线程的日志信息
 *
 * @author zhangpengju
 */
@Slf4j
public class LogContext {

    private static final ThreadLocal<LogInfo> LOG_INFO_THREAD_LOCAL = new ThreadLocal<>();

    public static void set(LogInfo logInfo) {
        LOG_INFO_THREAD_LOCAL.set(logInfo);
    }

    public static LogInfo get() {
        return LOG_INFO_THREAD_LOCAL.get();
    }

    public static void remove() {
        LOG_INFO_THREAD_LOCAL.remove();
    }

    public static void setExtra(String key, Object value) {
        LogInfo info = get();
        if (info != null) {
            if (info.getExtra() == null) {
                info.setExtra(new ConcurrentHashMap<>());
            }
            info.getExtra().put(key, value);
        }
    }

    public static Object getExtra(String key) {
        LogInfo info = get();
        if (info != null && info.getExtra() != null) {
            return info.getExtra().get(key);
        }
        return null;
    }

    /**
     * 日志信息内部类
     * <p>存储当前线程的日志上下文信息</p>
     */
    @Data
    public static class LogInfo {
        /** 用户 ID */
        private Long userId;
        
        /** 用户名 */
        private String username;
        
        /** 请求 IP 地址 */
        private String ipAddress;
        
        /** 用户代理 */
        private String userAgent;
        
        /** 请求 URI */
        private String requestUri;
        
        /** 请求方法 */
        private String requestMethod;
        
        /** 日志类型 */
        private LogType logType;
        
        /** 操作描述 */
        private String operation;
        
        /** 扩展字段 */
        private Map<String, Object> extra;
    }
}
