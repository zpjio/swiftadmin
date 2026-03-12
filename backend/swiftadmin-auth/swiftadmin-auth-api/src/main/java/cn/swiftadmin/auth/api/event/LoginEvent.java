package cn.swiftadmin.auth.api.event;

/**
 * 登录事件
 * <p>用于解耦登录日志记录等副作用操作</p>
 *
 * @param username 用户名
 * @param success 是否登录成功
 * @param message 登录消息
 * @param ipAddress IP 地址
 * @param userAgent User Agent
 * @param loginType 登录类型
 * @author zhangpengju
 * @since 2026-03-10
 */
public record LoginEvent(
        String username,
        boolean success,
        String message,
        String ipAddress,
        String userAgent,
        String loginType
) {

    /**
     * 创建登录成功事件
     */
    public static LoginEvent success(String username, String ipAddress, String userAgent, String loginType) {
        return new LoginEvent(username, true, "登录成功", ipAddress, userAgent, loginType);
    }

    /**
     * 创建登录失败事件
     */
    public static LoginEvent failure(String username, String message, String ipAddress, String userAgent, String loginType) {
        return new LoginEvent(username, false, message, ipAddress, userAgent, loginType);
    }

    /**
     * 创建 Token 刷新事件
     */
    public static LoginEvent tokenRefresh(String username, String ipAddress, String userAgent) {
        return new LoginEvent(username, true, "Token 刷新成功", ipAddress, userAgent, "refresh");
    }

}