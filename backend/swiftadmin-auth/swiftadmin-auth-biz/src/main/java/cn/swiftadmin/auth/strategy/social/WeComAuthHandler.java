package cn.swiftadmin.auth.strategy.social;

import cn.swiftadmin.auth.config.WeComProperties;
import cn.swiftadmin.auth.api.dto.SocialUserInfo;
import cn.swiftadmin.auth.api.exception.SocialAuthError;
import cn.swiftadmin.auth.api.exception.SocialAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 企业微信登录处理器
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComAuthHandler implements SocialAuthHandler {

    private final WeComProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

    @Override
    public String getSource() {
        return "wecom";
    }

    @Override
    public String getAuthUrl(String state) {
        return String.format(
                "https://open.work.weixin.qq.com/wwopen/sso/3rd_qrConnect" +
                        "?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_privateinfo&state=%s#wechat_redirect",
                properties.getCorpId(),
                encodeUrl(properties.getRedirectUri()),
                state
        );
    }

    @Override
    public SocialUserInfo getUserInfo(String authCode) {
        if (authCode == null || authCode.isEmpty()) {
            throw new SocialAuthException(SocialAuthError.AUTH_CODE_INVALID);
        }

        String accessToken = getAccessToken();
        String userInfoUrl = String.format("%s?access_token=%s&code=%s",
                properties.getUserInfoUrl(), accessToken, authCode);

        ResponseEntity<Map> response = restTemplate.getForEntity(userInfoUrl, Map.class);
        Map<String, Object> result = response.getBody();

        if (result == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "企业微信返回数据为空");
        }

        Integer errcode = (Integer) result.get("errcode");
        if (errcode != null && errcode != 0) {
            String errmsg = (String) result.get("errmsg");
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "企业微信授权失败：" + errmsg);
        }

        String userId = (String) result.get("UserId");
        if (userId != null && !userId.equals("not_exist")) {
            Map<String, Object> detail = getUserDetail(accessToken, userId);
            result.putAll(detail);
        }

        return buildSocialUserInfo(result);
    }

    @Override
    public String extractOpenId(SocialUserInfo userInfo) {
        return userInfo.getOpenId();
    }

    private SocialUserInfo buildSocialUserInfo(Map<String, Object> result) {
        return SocialUserInfo.builder()
                .openId((String) result.get("openId"))
                .nickname((String) result.get("nickname"))
                .avatar((String) result.get("avatar"))
                .email((String) result.get("email"))
                .mobile((String) result.get("mobile"))
                .source(getSource())
                .build();
    }

    private String getAccessToken() {
        String cacheKey = "wecom:access_token";
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String tokenUrl = String.format("%s?corpid=%s&corpsecret=%s",
                properties.getTokenUrl(), properties.getCorpId(), properties.getSecret());

        ResponseEntity<Map> response = restTemplate.getForEntity(tokenUrl, Map.class);
        Map<String, Object> result = response.getBody();

        if (result == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "获取企业微信 Access Token 失败");
        }

        Integer errcode = (Integer) result.get("errcode");
        if (errcode != null && errcode != 0) {
            String errmsg = (String) result.get("errmsg");
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "获取企业微信 Access Token 失败：" + errmsg);
        }

        String accessToken = (String) result.get("access_token");
        Integer expiresIn = (Integer) result.get("expires_in");

        if (accessToken != null && expiresIn != null) {
            redisTemplate.opsForValue().set(cacheKey, accessToken, expiresIn - 300, TimeUnit.SECONDS);
        }

        return accessToken;
    }

    private Map<String, Object> getUserDetail(String accessToken, String userId) {
        String detailUrl = String.format("%s?access_token=%s&userid=%s",
                properties.getUserDetailUrl(), accessToken, userId);

        ResponseEntity<Map> response = restTemplate.getForEntity(detailUrl, Map.class);
        Map<String, Object> result = response.getBody();

        if (result == null || result.get("errcode") != null) {
            log.warn("获取企业微信用户详情失败：userId={}", userId);
            return new HashMap<>();
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("openId", userId);
        userInfo.put("nickname", result.get("name"));
        userInfo.put("avatar", result.get("avatar"));
        userInfo.put("mobile", result.get("mobile"));
        userInfo.put("email", result.get("email"));

        return userInfo;
    }

    private String encodeUrl(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

}