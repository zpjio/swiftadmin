package cn.swiftadmin.auth.service.impl;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.event.LoginEvent;
import cn.swiftadmin.auth.api.service.AuthService;
import cn.swiftadmin.auth.api.service.SmsService;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.config.AuthSecurityProperties;
import cn.swiftadmin.auth.util.TokenManager;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserSessionService;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 * <p>统一处理认证相关逻辑：Token 管理、登出、短信验证码</p>
 * <p>增强版：支持 Token 类型验证、请求上下文验证、刷新次数限制</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;
    private final SmsService smsService;
    private final HttpServletRequest request;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final AuthSecurityProperties securityProperties;
    private final UserSessionService userSessionService;

    /** Token 刷新频率限制（秒）：同一用户两次刷新之间至少间隔 5 秒 */
    private static final int REFRESH_INTERVAL_SECONDS = 5;

    /** 单个用户最大连续刷新次数（防止 Token 无限续期） */
    private static final int MAX_REFRESH_COUNT_PER_DAY = 50;

    @Override
    public LoginVO login(LoginDTO dto) {
        String ipAddress = ServletUtils.getClientIp(request);
        String userAgent = ServletUtils.getUserAgent(request);

        SysUser user = userService.getByUsername(dto.getUsername());

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            eventPublisher.publishEvent(LoginEvent.failure(
                    dto.getUsername(), "认证失败", ipAddress, userAgent, "password"));
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        if (!user.getStatus().isEnabled()) {
            eventPublisher.publishEvent(LoginEvent.failure(
                    dto.getUsername(), "用户已禁用", ipAddress, userAgent, "password"));
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        eventPublisher.publishEvent(LoginEvent.success(
                user.getUsername(), ipAddress, userAgent, "password"));

        return buildLoginVO(user);
    }

    @Override
    public void logout() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            // 1. 获取当前请求的 Token 并加入黑名单
            String token = getTokenFromRequest();
            if (token != null) {
                tokenManager.addToBlacklist(token);
                log.debug("Token 已加入黑名单：userId={}", loginUser.getUserId());
                
                // 删除用户会话
                String sessionId = tokenManager.getJtiFromToken(token);
                if (sessionId != null) {
                    userSessionService.removeSession(sessionId);
                    log.debug("用户会话已删除：sessionId={}", sessionId);
                }
            }
            
            // 2. 清除用户信息缓存
            String userCacheKey = "user:info:" + loginUser.getUserId();
            redisTemplate.delete(userCacheKey);
            log.debug("用户信息缓存已清除：userId={}", loginUser.getUserId());
            
            // 3. 清除用户权限缓存
            String permCacheKey = "user:permissions:" + loginUser.getUserId();
            redisTemplate.delete(permCacheKey);
            log.debug("用户权限缓存已清除：userId={}", loginUser.getUserId());
            
            // 4. 清除 Token 刷新频率记录
            String refreshFreqKey = "token:refresh:freq:" + loginUser.getUsername();
            redisTemplate.delete(refreshFreqKey);
            
            // 5. 清除当日刷新次数记录
            String today = java.time.LocalDate.now().toString();
            String dailyCountKey = "token:refresh:count:" + loginUser.getUsername() + ":daily:" + today;
            redisTemplate.delete(dailyCountKey);
            
            log.info("用户登出成功：userId={}, username={}", loginUser.getUserId(), loginUser.getUsername());
        }
    }

    /**
     * 从请求头中获取 Token
     *
     * @return Token（不含 Bearer 前缀），无则返回 null
     */
    private String getTokenFromRequest() {
        String bearerToken = request.getHeader("Authorization");
        if (org.springframework.util.StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        // 1. 验证 Token 类型（必须是 Refresh Token）
        if (!tokenManager.isRefreshToken(refreshToken)) {
            log.warn("Token 刷新失败：提供的不是 Refresh Token");
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR.getCode(), "请使用 Refresh Token 刷新");
        }

        // 2. 验证 Token 有效性
        if (!tokenManager.validateToken(refreshToken)) {
            log.warn("Token 刷新失败：Refresh Token 无效或已过期");
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR.getCode(), "Refresh Token 无效或已过期");
        }

        // 3. 检查 Token 是否在黑名单中
        if (tokenManager.isTokenBlacklisted(refreshToken)) {
            log.warn("Token 刷新失败：Refresh Token 已在黑名单中，可能存在安全风险");
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR.getCode(), "Refresh Token 已失效，请重新登录");
        }

        // 4. 验证请求上下文（IP 和 User-Agent 一致性检查）
        validateRequestContext(refreshToken);

        // 5. 检查刷新频率限制
        checkRefreshFrequency(refreshToken);

        // 6. 获取用户名并验证用户状态
        String username = tokenManager.getUsernameFromToken(refreshToken);
        SysUser user = userService.getByUsername(username);

        if (user == null) {
            log.warn("Token 刷新失败：用户不存在，username={}", username);
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR.getCode(), "用户不存在");
        }
        if (!user.getStatus().isEnabled()) {
            log.warn("Token 刷新失败：用户已禁用，username={}", username);
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 7. 将旧的 Refresh Token 加入黑名单
        tokenManager.addToBlacklist(refreshToken);

        // 8. 记录刷新日志
        log.info("Token 刷新成功：username={}", username);

        // 9. 发布刷新事件
        String ipAddress = ServletUtils.getClientIp(request);
        String userAgent = ServletUtils.getUserAgent(request);
        eventPublisher.publishEvent(LoginEvent.tokenRefresh(
                username, ipAddress, userAgent));

        return buildLoginVO(user);
    }

    /**
     * 验证请求上下文（IP 和 User-Agent 一致性）
     */
    private void validateRequestContext(String refreshToken) {
        // 从 Redis 获取与该 Token 关联的请求上下文信息
        String jti = tokenManager.getJtiFromToken(refreshToken);
        if (jti == null) {
            log.warn("Token 刷新上下文验证失败：JTI 为空");
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR.getCode(), "Token 信息不完整");
        }

        // 获取存储的上下文信息
        String storedIp = redisTemplate.opsForValue().get("token:context:ip:" + jti);
        String storedUa = redisTemplate.opsForValue().get("token:context:ua:" + jti);
        
        // 获取当前请求信息
        String currentIp = ServletUtils.getClientIp(request);
        String currentUserAgent = ServletUtils.getUserAgent(request);

        // 如果是首次刷新（没有存储的上下文），保存当前上下文
        if (storedIp == null && storedUa == null) {
            log.info("首次刷新 Token，保存请求上下文：jti={}, ip={}, ua={}", jti, currentIp, currentUserAgent);
            saveRequestContext(jti, currentIp, currentUserAgent);
            return;
        }

        // 验证 IP 是否一致
        if (storedIp != null && !storedIp.equals(currentIp)) {
            log.warn("Token 刷新 IP 不一致：stored={}, current={}, jti={}. 可能存在 Token 被盗风险，已加入黑名单", 
                    storedIp, currentIp, jti);
            tokenManager.addToBlacklist(refreshToken);
            throw new BusinessException(ErrorCode.TOKEN_STOLEN);
        }

        // 验证 User-Agent 是否一致（宽松策略，只记录不阻止）
        if (storedUa != null && !storedUa.equals(currentUserAgent)) {
            log.warn("Token 刷新 User-Agent 不一致：stored={}, current={}, jti={}", 
                    storedUa, currentUserAgent, jti);
            // 注意：User-Agent 可能因浏览器升级而变化，这里只记录警告，不阻止刷新
        }

        log.debug("Token 刷新上下文验证通过：jti={}, ip={}", jti, currentIp);
    }

    /**
     * 保存请求上下文到 Redis
     *
     * @param jti JWT ID
     * @param ip IP 地址
     * @param ua User-Agent
     */
    private void saveRequestContext(String jti, String ip, String ua) {
        // 保存 IP 和 User-Agent，有效期与 Refresh Token 一致（7 天）
        long expireSeconds = securityProperties.getRefreshIntervalSeconds() * 2L;
        redisTemplate.opsForValue().set("token:context:ip:" + jti, ip, expireSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("token:context:ua:" + jti, ua, expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 检查 Token 刷新频率限制
     */
    private void checkRefreshFrequency(String refreshToken) {
        String username = tokenManager.getUsernameFromToken(refreshToken);
        String refreshKey = "token:refresh:freq:" + username;
        String countKey = "token:refresh:count:" + username;

        // 检查是否在最小间隔时间内
        String lastRefreshTime = redisTemplate.opsForValue().get(refreshKey);
        if (lastRefreshTime != null) {
            long currentTime = System.currentTimeMillis();
            long lastTime = Long.parseLong(lastRefreshTime);
            
            if (currentTime - lastTime < REFRESH_INTERVAL_SECONDS * 1000) {
                log.warn("Token 刷新频率过高：username={}, 时间间隔={}ms", username, currentTime - lastTime);
                throw new BusinessException(ErrorCode.TOKEN_REFRESH_TOO_FREQUENT);
            }
        }

        // 更新最后刷新时间
        redisTemplate.opsForValue().set(refreshKey, String.valueOf(System.currentTimeMillis()), 
                REFRESH_INTERVAL_SECONDS * 2, TimeUnit.SECONDS);

        // 检查当日刷新次数
        String today = java.time.LocalDate.now().toString();
        String dailyCountKey = countKey + ":daily:" + today;
        
        Long currentCount = redisTemplate.opsForValue().increment(dailyCountKey);
        if (currentCount == 1) {
            // 设置过期时间为明天零点
            redisTemplate.expire(dailyCountKey, getSecondsUntilTomorrow(), TimeUnit.SECONDS);
        }

        if (currentCount > MAX_REFRESH_COUNT_PER_DAY) {
            log.warn("Token 刷新次数超限：username={}, 当日次数={}", username, currentCount);
            throw new BusinessException(ErrorCode.TOKEN_REFRESH_LIMIT_EXCEEDED);
        }
    }

    /**
     * 获取距离明天零点的秒数
     */
    private long getSecondsUntilTomorrow() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime tomorrow = now.plusDays(1).withHour(0).withMinute(0).withSecond(0);
        return java.time.Duration.between(now, tomorrow).getSeconds();
    }

    @Override
    public void sendSms(String phone, String scene) {
        // 保持兼容性，但使用空参数调用（IP 和图形验证码由 Controller 直接处理）
        smsService.sendCode(phone, scene, null, null, null);
    }

    private LoginVO buildLoginVO(SysUser user) {
        return tokenManager.generateLoginTokens(user.getUsername());
    }

}