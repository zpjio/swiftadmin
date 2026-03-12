package cn.swiftadmin.auth.api.exception;

import lombok.Getter;

/**
 * 社交认证异常
 * <p>用于社交登录绑定过程中抛出，包含具体的错误类型</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Getter
public class SocialAuthException extends RuntimeException {

    /** 错误类型 */
    private final SocialAuthError error;

    /** 额外信息（可选） */
    private final String detail;

    public SocialAuthException(SocialAuthError error) {
        super(error.getMessage());
        this.error = error;
        this.detail = null;
    }

    public SocialAuthException(SocialAuthError error, String detail) {
        super(error.getMessage() + (detail != null ? ": " + detail : ""));
        this.error = error;
        this.detail = detail;
    }

    public SocialAuthException(SocialAuthError error, Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
        this.detail = null;
    }

    public SocialAuthException(SocialAuthError error, String detail, Throwable cause) {
        super(error.getMessage() + (detail != null ? ": " + detail : ""), cause);
        this.error = error;
        this.detail = detail;
    }

    /**
     * 获取错误码
     */
    public String getErrorCode() {
        return error.getCode();
    }

}