package cn.swiftadmin.auth.api.service;

/**
 * 短信服务接口
 * <p>提供短信验证码发送和验证功能，支持 IP 限流和图形验证码防护</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface SmsService {

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @param scene 使用场景：login|register|bind
     * @param ip 客户端 IP 地址
     * @param captchaId 图形验证码 ID（可选，触发图形验证码后必填）
     * @param captchaCode 图形验证码（可选，触发图形验证码后必填）
     */
    void sendCode(String phone, String scene, String ip, String captchaId, String captchaCode);

    /**
     * 发送验证码（简化版，无 IP 和图形验证码）
     *
     * @param phone 手机号
     * @param scene 使用场景
     */
    default void sendCode(String phone, String scene) {
        sendCode(phone, scene, null, null, null);
    }

    /**
     * 验证验证码
     *
     * @param phone 手机号
     * @param code 验证码
     * @param scene 使用场景
     * @return true=验证通过，false=验证失败
     */
    boolean verifyCode(String phone, String code, String scene);

    /**
     * 检查是否需要图形验证码
     *
     * @param ip 客户端 IP
     * @return true=需要图形验证码
     */
    boolean isCaptchaRequired(String ip);

}
