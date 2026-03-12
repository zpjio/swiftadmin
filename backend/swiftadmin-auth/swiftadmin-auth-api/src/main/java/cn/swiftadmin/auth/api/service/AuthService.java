package cn.swiftadmin.auth.api.service;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.vo.LoginVO;

/**
 * 认证服务接口
 * <p>提供用户登录、登出、Token 刷新、短信验证码功能</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param dto 登录参数（用户名、密码）
     * @return 登录结果（Access Token、Refresh Token）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户登出
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
