package cn.swiftadmin.auth.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一社交用户信息 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
public class SocialUserInfo {

    private String openId;
    private String unionId;
    private String nickname;
    private String avatar;
    private String email;
    private String mobile;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;

    /** 来源平台 */
    private String source;

    /** 额外信息（平台特有字段） */
    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    /**
     * 添加额外信息
     */
    public SocialUserInfo addExtra(String key, Object value) {
        if (this.extra == null) {
            this.extra = new HashMap<>();
        }
        this.extra.put(key, value);
        return this;
    }

    /**
     * 获取额外信息
     */
    public Object getExtra(String key) {
        return this.extra != null ? this.extra.get(key) : null;
    }

    /**
     * 转换为 Map 格式（兼容旧代码）
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (openId != null) map.put("openId", openId);
        if (unionId != null) map.put("unionId", unionId);
        if (nickname != null) map.put("nickname", nickname);
        if (avatar != null) map.put("avatar", avatar);
        if (email != null) map.put("email", email);
        if (mobile != null) map.put("mobile", mobile);
        if (accessToken != null) map.put("accessToken", accessToken);
        if (refreshToken != null) map.put("refreshToken", refreshToken);
        if (expiresIn != null) map.put("expiresIn", expiresIn);
        if (source != null) map.put("source", source);
        if (extra != null) map.putAll(extra);
        return map;
    }

    /**
     * 从 Map 创建（兼容旧代码）
     */
    public static SocialUserInfo fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        SocialUserInfoBuilder builder = SocialUserInfo.builder()
                .openId(getStringValue(map, "openId", "open_id"))
                .unionId(getStringValue(map, "unionId", "union_id"))
                .nickname(getStringValue(map, "nickname", "nick", "name"))
                .avatar(getStringValue(map, "avatar", "avatarUrl", "headimgurl"))
                .email(getStringValue(map, "email"))
                .mobile(getStringValue(map, "mobile", "phone"))
                .accessToken(getStringValue(map, "accessToken", "access_token"))
                .refreshToken(getStringValue(map, "refreshToken", "refresh_token"))
                .expiresIn(getLongValue(map, "expiresIn", "expires_in"))
                .source(getStringValue(map, "source"));

        // 提取额外信息（排除已映射的字段）
        Map<String, Object> extra = new HashMap<>(map);
        extra.remove("openId");
        extra.remove("open_id");
        extra.remove("unionId");
        extra.remove("union_id");
        extra.remove("nickname");
        extra.remove("nick");
        extra.remove("name");
        extra.remove("avatar");
        extra.remove("avatarUrl");
        extra.remove("headimgurl");
        extra.remove("email");
        extra.remove("mobile");
        extra.remove("phone");
        extra.remove("accessToken");
        extra.remove("access_token");
        extra.remove("refreshToken");
        extra.remove("refresh_token");
        extra.remove("expiresIn");
        extra.remove("expires_in");
        extra.remove("source");
        builder.extra(extra);

        return builder.build();
    }

    private static String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private static Long getLongValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                try {
                    return Long.parseLong(value.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

}