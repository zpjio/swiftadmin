package cn.swiftadmin.common.security.service.impl;

import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public UserSessionServiceImpl(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void createSession(String sessionId, LoginUser loginUser, String ip, String userAgent, long expireSeconds) {
        String tokenKey = RedisKeyConstants.loginToken(sessionId);
        String extraKey = RedisKeyConstants.loginSessionExtra(sessionId);
        String userTokensKey = RedisKeyConstants.USER_TOKENS + loginUser.getUserId();

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
        String key = RedisKeyConstants.loginToken(sessionId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasLength(json)) {
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
        String key = RedisKeyConstants.loginSessionExtra(sessionId);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (CollectionUtils.isEmpty(entries)) {
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
        String tokenKey = RedisKeyConstants.loginToken(sessionId);
        String extraKey = RedisKeyConstants.loginSessionExtra(sessionId);
        String json = stringRedisTemplate.opsForValue().getAndDelete(tokenKey);

        if (json != null) {
            try {
                LoginUser loginUser = objectMapper.readValue(json, LoginUser.class);
                stringRedisTemplate.delete(extraKey);
                if (loginUser != null && loginUser.getUserId() != null) {
                    stringRedisTemplate.opsForSet().remove(RedisKeyConstants.USER_TOKENS + loginUser.getUserId(), sessionId);
                }
                log.info("删除用户会话成功: sessionId={}, userId={}", sessionId, loginUser != null ? loginUser.getUserId() : null);
                return true;
            } catch (JsonProcessingException e) {
                log.error("反序列化 LoginUser 失败", e);
            }
        }

        // 清理可能残留的 key
        stringRedisTemplate.delete(extraKey);
        log.debug("会话不存在或已过期: sessionId={}", sessionId);
        return false;
    }

    @Override
    public List<String> getUserSessionIds(Long userId) {
        Set<String> tokens = stringRedisTemplate.opsForSet().members(RedisKeyConstants.USER_TOKENS + userId);
        if (CollectionUtils.isEmpty(tokens)) {
            log.debug("用户 {} 没有活跃会话", userId);
            return new ArrayList<>();
        }
        // 过滤掉已过期的会话
        List<String> validSessions = new ArrayList<>();
        for (String sessionId : tokens) {
            Boolean exists = stringRedisTemplate.hasKey(RedisKeyConstants.loginToken(sessionId));
            if (Boolean.TRUE.equals(exists)) {
                validSessions.add(sessionId);
            } else {
                // 清理过期会话记录
                stringRedisTemplate.opsForSet().remove(RedisKeyConstants.USER_TOKENS + userId, sessionId);
                log.debug("清理过期会话记录: userId={}, sessionId={}", userId, sessionId);
            }
        }
        log.debug("用户 {} 有效会话数: {}", userId, validSessions.size());
        return validSessions;
    }

    @Override
    public List<String> getAllSessionIds() {
        Set<String> keys = stringRedisTemplate.keys(RedisKeyConstants.LOGIN_TOKEN + "*");
        if (CollectionUtils.isEmpty(keys)) {
            return new ArrayList<>();
        }
        return keys.stream()
                .map(key -> key.replace(RedisKeyConstants.LOGIN_TOKEN, ""))
                .toList();
    }

    @Override
    public long count() {
        Set<String> keys = stringRedisTemplate.keys(RedisKeyConstants.LOGIN_TOKEN + "*");
        return keys != null ? keys.size() : 0;
    }

    @Override
    public void refreshSession(String sessionId, long expireSeconds) {
        String tokenKey = RedisKeyConstants.loginToken(sessionId);
        String extraKey = RedisKeyConstants.loginSessionExtra(sessionId);

        Long ttl = stringRedisTemplate.getExpire(tokenKey, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            stringRedisTemplate.expire(tokenKey, expireSeconds, TimeUnit.SECONDS);
            stringRedisTemplate.expire(extraKey, expireSeconds, TimeUnit.SECONDS);

            LoginUser loginUser = getSession(sessionId);
            if (loginUser != null) {
                stringRedisTemplate.expire(RedisKeyConstants.USER_TOKENS + loginUser.getUserId(), expireSeconds, TimeUnit.SECONDS);
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
