package cn.swiftadmin.common.security.service.impl;

import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户会话服务实现类
 * <p>基于 Redis 实现用户会话管理</p>
 *
 * @author zhangpengju
 * @since 2026-03-12
 */
@Slf4j
@Service
public class UserSessionServiceImpl implements UserSessionService {

    private static final String LOGIN_TOKEN_KEY = "swiftadmin:login:token:";
    private static final String USER_TOKENS_KEY = "swiftadmin:login:user_tokens:";
    private static final String SESSION_EXTRA_KEY = "swiftadmin:login:extra:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public UserSessionServiceImpl(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void createSession(String sessionId, LoginUser loginUser, String ip, String userAgent, long expireSeconds) {
        String tokenKey = LOGIN_TOKEN_KEY + sessionId;
        String extraKey = SESSION_EXTRA_KEY + sessionId;
        String userTokensKey = USER_TOKENS_KEY + loginUser.getUserId();

        try {
            String loginUserJson = objectMapper.writeValueAsString(loginUser);
            stringRedisTemplate.opsForValue().set(tokenKey, loginUserJson, expireSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("序列化 LoginUser 失败", e);
            throw new RuntimeException("序列化 LoginUser 失败", e);
        }

        Map<String, String> extra = new HashMap<>();
        extra.put("ip", ip != null ? ip : "");
        extra.put("userAgent", userAgent != null ? userAgent : "");
        extra.put("loginTime", String.valueOf(System.currentTimeMillis()));
        stringRedisTemplate.opsForHash().putAll(extraKey, extra);
        stringRedisTemplate.expire(extraKey, expireSeconds, TimeUnit.SECONDS);

        stringRedisTemplate.opsForSet().add(userTokensKey, sessionId);
        stringRedisTemplate.expire(userTokensKey, expireSeconds, TimeUnit.SECONDS);

        log.debug("创建用户会话: sessionId={}, userId={}, ip={}", sessionId, loginUser.getUserId(), ip);
    }

    @Override
    public LoginUser getSession(String sessionId) {
        String key = LOGIN_TOKEN_KEY + sessionId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginUser.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化 LoginUser 失败: {}", json, e);
            return null;
        }
    }

    @Override
    public SessionExtra getSessionExtra(String sessionId) {
        String key = SESSION_EXTRA_KEY + sessionId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        String ip = (String) entries.get("ip");
        String userAgent = (String) entries.get("userAgent");
        String loginTimeStr = (String) entries.get("loginTime");
        Long loginTimeMs = loginTimeStr != null ? Long.parseLong(loginTimeStr) : null;

        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        LocalDateTime loginTime = loginTimeMs != null 
                ? LocalDateTime.ofEpochSecond(loginTimeMs / 1000, 0, ZoneOffset.ofHours(8)) 
                : LocalDateTime.now();
        LocalDateTime expireTime = ttl != null && ttl > 0 
                ? LocalDateTime.now().plusSeconds(ttl) 
                : null;

        return new SessionExtra(
                ip,
                null,
                parseBrowser(userAgent),
                parseOs(userAgent),
                loginTime,
                expireTime
        );
    }

    @Override
    public boolean removeSession(String sessionId) {
        String tokenKey = LOGIN_TOKEN_KEY + sessionId;
        String json = stringRedisTemplate.opsForValue().get(tokenKey);

        if (json != null) {
            try {
                LoginUser loginUser = objectMapper.readValue(json, LoginUser.class);
                stringRedisTemplate.delete(tokenKey);
                stringRedisTemplate.delete(SESSION_EXTRA_KEY + sessionId);
                if (loginUser != null && loginUser.getUserId() != null) {
                    stringRedisTemplate.opsForSet().remove(USER_TOKENS_KEY + loginUser.getUserId(), sessionId);
                }
                log.debug("删除用户会话: sessionId={}", sessionId);
                return true;
            } catch (JsonProcessingException e) {
                log.error("反序列化 LoginUser 失败", e);
            }
        }

        stringRedisTemplate.delete(tokenKey);
        stringRedisTemplate.delete(SESSION_EXTRA_KEY + sessionId);
        return false;
    }

    @Override
    public List<String> getUserSessionIds(Long userId) {
        Set<String> tokens = stringRedisTemplate.opsForSet().members(USER_TOKENS_KEY + userId);
        if (tokens == null || tokens.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tokens);
    }

    @Override
    public List<String> getAllSessionIds() {
        Set<String> keys = stringRedisTemplate.keys(LOGIN_TOKEN_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }
        return keys.stream()
                .map(key -> key.replace(LOGIN_TOKEN_KEY, ""))
                .toList();
    }

    @Override
    public long count() {
        Set<String> keys = stringRedisTemplate.keys(LOGIN_TOKEN_KEY + "*");
        return keys != null ? keys.size() : 0;
    }

    @Override
    public void refreshSession(String sessionId, long expireSeconds) {
        String tokenKey = LOGIN_TOKEN_KEY + sessionId;
        String extraKey = SESSION_EXTRA_KEY + sessionId;

        Long ttl = stringRedisTemplate.getExpire(tokenKey, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            stringRedisTemplate.expire(tokenKey, expireSeconds, TimeUnit.SECONDS);
            stringRedisTemplate.expire(extraKey, expireSeconds, TimeUnit.SECONDS);

            LoginUser loginUser = getSession(sessionId);
            if (loginUser != null) {
                stringRedisTemplate.expire(USER_TOKENS_KEY + loginUser.getUserId(), expireSeconds, TimeUnit.SECONDS);
            }
            log.debug("刷新会话过期时间: sessionId={}, expireSeconds={}", sessionId, expireSeconds);
        }
    }

    private String parseBrowser(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        if (userAgent.contains("Edg/")) {
            return "Edge";
        } else if (userAgent.contains("Chrome/")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox/")) {
            return "Firefox";
        } else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome")) {
            return "Safari";
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/")) {
            return "IE";
        }
        return "Unknown";
    }

    private String parseOs(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        if (userAgent.contains("Windows NT 10")) {
            return "Windows 10";
        } else if (userAgent.contains("Windows NT 6.3")) {
            return "Windows 8.1";
        } else if (userAgent.contains("Windows NT 6.2")) {
            return "Windows 8";
        } else if (userAgent.contains("Windows NT 6.1")) {
            return "Windows 7";
        } else if (userAgent.contains("Mac OS X")) {
            return "Mac OS X";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        }
        return "Unknown";
    }
}
