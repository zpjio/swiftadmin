package cn.swiftadmin.auth.api.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 统一登录响应 VO
 * <p>用户登录成功后返回的 Token 信息，支持第三方登录绑定流程</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Data
@Builder
public class LoginVO {

    /** 访问 Token（用于接口认证） */
    private String accessToken;

    /** 刷新 Token（用于刷新访问 Token） */
    private String refreshToken;

    /** Token 过期时间（秒） */
    private Long expiresIn;

    /** Token 类型 */
    private String tokenType;

    /** 是否需要绑定（第三方首次登录） */
    private Boolean needBind;

    /** 临时 Token（用于后续绑定流程） */
    private String tempToken;

    /** 用户信息（首次登录时返回） */
    private UserInfo userInfo;

    // ========== 密码策略相关 ==========

    /** 是否需要强制修改密码 */
    private Boolean requirePasswordChange;

    /** 密码是否过期 */
    private Boolean passwordExpired;

    /** 密码剩余有效天数（用于提前提醒） */
    private Integer passwordRemainingDays;

    /**
     * 第三方登录用户信息
     */
    @Data
    @Builder
    public static class UserInfo {
        /** 昵称 */
        private String nickname;
        /** 头像 URL */
        private String avatar;
        /** 来源平台 */
        private String source;
    }

}
