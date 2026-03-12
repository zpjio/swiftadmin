package cn.swiftadmin.auth.api.exception;

import lombok.Getter;

/**
 * 社交认证错误枚举
 * <p>定义社交登录绑定过程中可能出现的错误类型</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Getter
public enum SocialAuthError {

    /** 临时 Token 已过期 */
    TEMP_TOKEN_EXPIRED("A001", "临时授权已过期，请重新授权"),

    /** 临时 Token 已使用 */
    TEMP_TOKEN_USED("A002", "临时授权已使用，请重新授权"),

    /** 社交账号已被其他用户绑定 */
    BIND_ALREADY_EXISTS("A003", "该社交账号已被其他用户绑定"),

    /** 用户已绑定同来源账号 */
    USER_ALREADY_BOUND("A004", "您已绑定该来源的社交账号"),

    /** 第三方平台错误 */
    PLATFORM_ERROR("A005", "第三方平台授权失败"),

    /** State 参数无效 */
    STATE_INVALID("A006", "授权状态无效"),

    /** State 参数已过期 */
    STATE_EXPIRED("A007", "授权已过期，请重新获取登录链接"),

    /** State 来源不匹配 */
    STATE_SOURCE_MISMATCH("A008", "授权来源不匹配"),

    /** 绑定操作进行中 */
    OPERATION_IN_PROGRESS("A009", "操作进行中，请稍后重试"),

    /** 未找到绑定关系 */
    BIND_NOT_FOUND("A010", "未找到该社交账号绑定关系"),

    /** 解绑验证失败 */
    UNBIND_VERIFY_FAILED("A011", "解绑验证失败，请检查密码或验证码"),

    /** 不支持的社交平台 */
    UNSUPPORTED_PLATFORM("A012", "不支持的社交登录平台"),

    /** 授权码无效 */
    AUTH_CODE_INVALID("A013", "授权码无效或已过期");

    /** 错误码 */
    private final String code;

    /** 错误消息 */
    private final String message;

    SocialAuthError(String code, String message) {
        this.code = code;
        this.message = message;
    }

}