package cn.swiftadmin.auth.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * OAuth State 参数 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
public class OAuthStateDTO {

    private String source;
    private String sessionId;
    private Long timestamp;
    private String nonce;
    private String action;
    private Long bindUserId;

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_BIND = "bind";
    public static final long VALID_PERIOD = 10 * 60 * 1000;

    public boolean isExpired() {
        if (timestamp == null) {
            return true;
        }
        return System.currentTimeMillis() - timestamp > VALID_PERIOD;
    }

}