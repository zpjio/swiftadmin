package cn.swiftadmin.auth.api.service;

import cn.swiftadmin.auth.api.vo.CaptchaVO;

/**
 * 图形验证码服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface CaptchaService {

    /**
     * 生成验证码
     *
     * @return 验证码信息
     */
    CaptchaVO generate();

    /**
     * 验证验证码
     *
     * @param captchaId 验证码 ID
     * @param code 用户输入的验证码
     * @return true=验证通过
     */
    boolean verify(String captchaId, String code);

    /**
     * 检查是否需要图形验证码
     *
     * @param ip 客户端 IP
     * @param threshold 触发阈值
     * @return true=需要验证码
     */
    boolean isCaptchaRequired(String ip, int threshold);
}