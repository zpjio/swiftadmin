package cn.swiftadmin.auth.context;

import cn.swiftadmin.auth.api.enums.LoginType;
import lombok.Builder;
import lombok.Getter;

/**
 * 登录上下文
 * <p>封装登录请求的上下文信息，减少依赖注入数量</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Getter
@Builder
public class LoginContext {

    /** 客户端 IP 地址 */
    private final String ip;

    /** User-Agent */
    private final String userAgent;

    /** 登录类型 */
    private final LoginType loginType;

    /** 登录来源（社交登录平台标识） */
    private final String source;

    /** 用户名（用于日志和事件） */
    private String username;

    /**
     * 创建密码登录上下文
     */
    public static LoginContext ofPassword(String ip, String userAgent) {
        return LoginContext.builder()
                .ip(ip)
                .userAgent(userAgent)
                .loginType(LoginType.PASSWORD)
                .source("password")
                .build();
    }

    /**
     * 创建短信登录上下文
     */
    public static LoginContext ofSms(String ip, String userAgent) {
        return LoginContext.builder()
                .ip(ip)
                .userAgent(userAgent)
                .loginType(LoginType.SMS)
                .source("sms")
                .build();
    }

    /**
     * 创建社交登录上下文
     */
    public static LoginContext ofSocial(String ip, String userAgent, String source) {
        return LoginContext.builder()
                .ip(ip)
                .userAgent(userAgent)
                .loginType(null)
                .source(source)
                .build();
    }

    /**
     * 设置用户名
     */
    public LoginContext withUsername(String username) {
        this.username = username;
        return this;
    }
}