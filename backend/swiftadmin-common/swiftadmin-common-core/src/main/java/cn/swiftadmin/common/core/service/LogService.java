package cn.swiftadmin.common.core.service;

import cn.swiftadmin.common.core.enums.LogType;
import cn.swiftadmin.common.core.enums.LogStatus;

/**
 * 日志服务接口
 *
 * @author zhangpengju
 */
public interface LogService {

    void record(LogType logType, LogStatus status, String message, Object... extraParams);

    void recordLogin(String username, String ipAddress, String userAgent, LogStatus status, String message);

    void recordOperation(String username, String module, String operation, String method,
                        String requestUri, String ipAddress, LogStatus status, String message, Long executionTime);

}
