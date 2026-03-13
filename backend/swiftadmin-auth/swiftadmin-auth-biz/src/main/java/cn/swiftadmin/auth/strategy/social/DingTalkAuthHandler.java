package cn.swiftadmin.auth.strategy.social;

import cn.swiftadmin.auth.config.DingTalkProperties;
import cn.swiftadmin.auth.api.dto.SocialUserInfo;
import cn.swiftadmin.auth.api.exception.SocialAuthError;
import cn.swiftadmin.auth.api.exception.SocialAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉登录处理器
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkAuthHandler implements SocialAuthHandler {

    private final DingTalkProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

    @Override
    public String getSource() {
        return "dingtalk";
    }

    @Override
    public String getAuthUrl(String state) {
        return String.format(
                "https://oapi.dingtalk.com/connect/qrconnect" +
                        "?appid=%s&response_type=code&redirect_uri=%s&scope=snsapi_login&state=%s",
                properties.getAppKey(),
                encodeUrl(properties.getRedirectUri()),
                state
        );
    }

    @Override
    public SocialUserInfo getUserInfo(String authCode) {
        if (!StringUtils.hasLength(authCode)) {
            throw new SocialAuthException(SocialAuthError.AUTH_CODE_INVALID);
        }

        String accessToken = getAccessToken();
        long timestamp = System.currentTimeMillis();
        String signature = generateSignature(timestamp, properties.getAppSecret());

        String userInfoUrl = String.format("%s?accessKey=%s&timestamp=%s&signature=%s",
                properties.getUserInfoUrl(), properties.getAppKey(), timestamp, signature);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tmp_auth_code", authCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-acs-dingtalk-access-token", accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(userInfoUrl, request, Map.class);
        Map<String, Object> result = response.getBody();

        if (result == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "钉钉返回数据为空");
        }

        Integer code = (Integer) result.get("code");
        if (code != null && code != 0) {
            String message = (String) result.get("message");
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "钉钉授权失败：" + message);
        }

        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "钉钉用户数据为空");
        }

        return buildSocialUserInfo(data);
    }

    @Override
    public String extractOpenId(SocialUserInfo userInfo) {
        String openId = userInfo.getOpenId();
        return openId != null ? openId : userInfo.getUnionId();
    }

    private SocialUserInfo buildSocialUserInfo(Map<String, Object> data) {
        return SocialUserInfo.builder()
                .openId((String) data.get("openid"))
                .unionId((String) data.get("unionid"))
                .nickname((String) data.get("nick"))
                .avatar((String) data.get("avatarUrl"))
                .mobile((String) data.get("mobile"))
                .source(getSource())
                .build();
    }

    private String getAccessToken() {
        String cacheKey = "dingtalk:access_token";
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String tokenUrl = String.format("%s?appkey=%s&appsecret=%s",
                properties.getTokenUrl(), properties.getAppKey(), properties.getAppSecret());

        ResponseEntity<Map> response = restTemplate.getForEntity(tokenUrl, Map.class);
        Map<String, Object> result = response.getBody();

        if (result == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "获取钉钉 Access Token 失败");
        }

        Integer errcode = (Integer) result.get("errcode");
        if (errcode != null && errcode != 0) {
            String errmsg = (String) result.get("errmsg");
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "获取钉钉 Access Token 失败：" + errmsg);
        }

        String accessToken = (String) result.get("access_token");
        Integer expiresIn = (Integer) result.get("expires_in");

        if (accessToken != null && expiresIn != null) {
            redisTemplate.opsForValue().set(cacheKey, accessToken, expiresIn - 300, TimeUnit.SECONDS);
        }

        return accessToken;
    }

    private String generateSignature(long timestamp, String appSecret) {
        try {
            String stringToSign = timestamp + "\n" + appSecret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("生成钉钉签名失败", e);
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "生成钉钉签名失败");
        }
    }

    private String encodeUrl(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

}