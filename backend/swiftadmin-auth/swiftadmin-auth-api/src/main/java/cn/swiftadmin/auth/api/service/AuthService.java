package cn.swiftadmin.auth.api.service;

import cn.swiftadmin.auth.api.vo.LoginVO;

/**
 * 认证服务接口
 * <p>提供 Token 管理功能：登出、刷新、短信验证码</p>
 * <p>登录认证由 {@link cn.swiftadmin.auth.strategy.AuthStrategy} 策略模式处理</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
public interface AuthService {

    /**
     * 用户登出
     * <p>将当前 Token 加入黑名单，清除相关缓存</p>
     */
    void logout();

    /**
     * 刷新 Token
     *
     * @param refreshToken 刷新 Token
     * @return 新的 Token 对
     */
    LoginVO refresh(String refreshToken);

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param scene 场景
     */
    void sendSms(String phone, String scene);

}
