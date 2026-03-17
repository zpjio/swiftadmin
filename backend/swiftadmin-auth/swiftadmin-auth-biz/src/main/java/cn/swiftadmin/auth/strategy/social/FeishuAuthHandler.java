package cn.swiftadmin.auth.strategy.social;

import cn.swiftadmin.auth.config.FeishuProperties;
import cn.swiftadmin.auth.api.dto.SocialUserInfo;
import cn.swiftadmin.auth.api.exception.SocialAuthError;
import cn.swiftadmin.auth.api.exception.SocialAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 飞书登录处理器
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuAuthHandler implements SocialAuthHandler {

    private final FeishuProperties properties;
    private final RestTemplate restTemplate;

    @Override
    public String getSource() {
        return "feishu";
    }

    @Override
    public String getAuthUrl(String state) {
        return String.format(
                "https://open.feishu.cn/open-apis/authen/v1/authorize" +
                        "?app_id=%s&redirect_uri=%s&response_type=code&state=%s",
                properties.getAppId(),
                encodeUrl(properties.getRedirectUri()),
                state
        );
    }

    @Override
    public SocialUserInfo getUserInfo(String authCode) {
        if (!StringUtils.hasLength(authCode)) {
            throw new SocialAuthException(SocialAuthError.AUTH_CODE_INVALID);
        }

        Map<String, Object> tokenResult = getAccessToken(authCode);
        String accessToken = (String) tokenResult.get("access_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                properties.getUserInfoUrl(),
                HttpMethod.GET,
                request,
                Map.class
        );
        Map<String, Object> result = response.getBody();

        if (result == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "飞书返回数据为空");
        }

        Integer code = (Integer) result.get("code");
        if (code != null && code != 0) {
            String message = (String) result.get("msg");
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "飞书授权失败：" + message);
        }

        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "飞书用户数据为空");
        }

        Map<String, Object> userInfo = (Map<String, Object>) data.get("user_info");
        if (userInfo == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "飞书用户信息为空");
        }

        return buildSocialUserInfo(userInfo, accessToken, tokenResult);
    }

    @Override
    public String extractOpenId(SocialUserInfo userInfo) {
        return userInfo.getOpenId();
    }

    private SocialUserInfo buildSocialUserInfo(Map<String, Object> userInfo, String accessToken, Map<String, Object> tokenResult) {
        return SocialUserInfo.builder()
                .openId((String) userInfo.get("open_id"))
                .unionId((String) userInfo.get("union_id"))
                .nickname((String) userInfo.get("nickname"))
                .avatar((String) userInfo.get("avatar_url"))
                .email((String) userInfo.get("email"))
                .mobile((String) userInfo.get("mobile"))
                .accessToken(accessToken)
                .refreshToken((String) tokenResult.get("refresh_token"))
                .expiresIn(getExpiresIn(tokenResult))
                .source(getSource())
                .build();
    }

    private Long getExpiresIn(Map<String, Object> tokenResult) {
        Object expiresIn = tokenResult.get("expires_in");
        if (expiresIn instanceof Number) {
            return ((Number) expiresIn).longValue();
        }
        return null;
    }

    private Map<String, Object> getAccessToken(String authCode) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("grant_type", "authorization_code");
        requestBody.put("code", authCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                properties.getAccessTokenUrl(),
                request,
                Map.class
        );
        Map<String, Object> result = response.getBody();

        if (result == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "获取飞书 Access Token 失败");
        }

        Integer code = (Integer) result.get("code");
        if (code != null && code != 0) {
            String message = (String) result.get("msg");
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "获取飞书 Access Token 失败：" + message);
        }

        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            throw new SocialAuthException(SocialAuthError.PLATFORM_ERROR, "飞书 Token 数据为空");
        }

        return data;
    }

    private String encodeUrl(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

}