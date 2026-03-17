package cn.swiftadmin.auth.service.impl;

import cn.swiftadmin.auth.api.event.LoginEvent;
import cn.swiftadmin.auth.api.service.AuthService;
import cn.swiftadmin.auth.api.service.SmsService;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.config.AuthSecurityProperties;
import cn.swiftadmin.auth.util.TokenManager;
import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.core.util.SensitiveUtils;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.UserSessionService;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 * <p>处理 Token 管理：登出、刷新、短信验证码</p>
 * <p>登录认证由 {@link cn.swiftadmin.auth.strategy.AuthStrategy} 策略模式处理</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final TokenManager tokenManager;
    private final SmsService smsService;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final AuthSecurityProperties securityProperties;
    private final UserSessionService userSessionService;

    @Override
    public void logout() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
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
            String userCacheKey = RedisKeyConstants.userInfo(loginUser.getUserId());
            redisTemplate.delete(userCacheKey);
            log.debug("用户信息缓存已清除：userId={}", loginUser.getUserId());

            // 3. 清除用户权限缓存
            String permCacheKey = RedisKeyConstants.userPermission(loginUser.getUserId());
            redisTemplate.delete(permCacheKey);
            log.debug("用户权限缓存已清除：userId={}", loginUser.getUserId());

            // 4. 清除 Token 刷新频率记录
            String refreshFreqKey = RedisKeyConstants.tokenRefreshFreq(loginUser.getUsername());
            redisTemplate.delete(refreshFreqKey);

            // 5. 清除当日刷新次数记录
            String today = LocalDate.now().toString();
            String dailyCountKey = RedisKeyConstants.tokenRefreshDailyCount(loginUser.getUsername(), today);
            redisTemplate.delete(dailyCountKey);
            
            log.info("用户登出成功：userId={}, username={}", loginUser.getUserId(), SensitiveUtils.maskUsername(loginUser.getUsername()));
        }
    }

    /**
     * 从请求头中获取 Token
     *
     * @return Token（不含 Bearer 前缀），无则返回 null
     */
    private String getTokenFromRequest() {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return null;
        }
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        // 1. 验证 Token 类型（必须是 Refresh Token）
        if (!tokenManager.isRefreshToken(refreshToken)) {
            log.warn("Token 刷新失败：提供的不是 Refresh Token");
            throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "请使用 Refresh Token 刷新");
        }

        // 2. 验证 Token 有效性
        if (!tokenManager.validateToken(refreshToken)) {
            log.warn("Token 刷新失败：Refresh Token 无效或已过期");
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 检查 Token 是否在黑名单中
        if (tokenManager.isTokenBlacklisted(refreshToken)) {
            log.warn("Token 刷新失败：Refresh Token 已在黑名单中，可能存在安全风险");
            throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "Refresh Token 已失效，请重新登录");
        }

        // 4. 验证请求上下文（IP 和 User-Agent 一致性检查）
        validateRequestContext(refreshToken);

        // 5. 检查刷新频率限制
        checkRefreshFrequency(refreshToken);

        // 6. 获取用户名并验证用户状态
        String username = tokenManager.getUsernameFromToken(refreshToken);
        SysUser user = userService.getByUsername(username);

        if (user == null) {
            log.warn("Token 刷新失败：用户不存在，username={}", SensitiveUtils.maskUsername(username));
            throw new BusinessException(ErrorCode.LOGIN_FAILED.getCode(), "用户不存在");
        }
        if (!user.getStatus().isEnabled()) {
            log.warn("Token 刷新失败：用户已禁用，username={}", SensitiveUtils.maskUsername(username));
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 7. 将旧的 Refresh Token 加入黑名单
        tokenManager.addToBlacklist(refreshToken);

        // 8. 生成新的 Token
        LoginVO loginVO = buildLoginVO(user);

        // 9. 为新 Refresh Token 建立上下文
        String newRefreshJti = tokenManager.getJtiFromToken(loginVO.getRefreshToken());
        String currentIp = ServletUtils.getClientIp();
        String currentUserAgent = ServletUtils.getUserAgent();
        if (newRefreshJti != null) {
            saveRequestContext(newRefreshJti, currentIp, currentUserAgent);
        }

        // 10. 记录刷新日志
        log.info("Token 刷新成功：username={}", SensitiveUtils.maskUsername(username));

        // 11. 发布刷新事件
        eventPublisher.publishEvent(LoginEvent.tokenRefresh(
                username, currentIp, currentUserAgent));

        return loginVO;
    }

    /**
     * 验证请求上下文（IP 和 User-Agent 一致性）
     * <p>改进：采用可配置的验证策略，避免用户切换网络被强制登出</p>
     */
    private void validateRequestContext(String refreshToken) {
        // 从 Redis 获取与该 Token 关联的请求上下文信息
        String jti = tokenManager.getJtiFromToken(refreshToken);
        if (jti == null) {
            log.warn("Token 刷新上下文验证失败：JTI 为空");
            throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "Token 信息不完整");
        }

        // 获取存储的上下文信息
        String storedIp = redisTemplate.opsForValue().get(RedisKeyConstants.tokenContextIp(jti));
        String storedUa = redisTemplate.opsForValue().get(RedisKeyConstants.tokenContextUa(jti));

        // 获取当前请求信息
        String currentIp = ServletUtils.getClientIp();
        String currentUserAgent = ServletUtils.getUserAgent();

        // 如果是首次刷新（没有存储的上下文），保存当前上下文
        if (storedIp == null && storedUa == null) {
            log.info("首次刷新 Token，保存请求上下文：jti={}, ip={}, ua={}", jti, currentIp, currentUserAgent);
            saveRequestContext(jti, currentIp, currentUserAgent);
            return;
        }

        // 检查 IP 是否变化
        boolean ipChanged = storedIp != null && !storedIp.equals(currentIp);

        if (ipChanged) {
            handleIpChange(jti, storedIp, currentIp, refreshToken);
        }

        // 验证 User-Agent 是否一致（宽松策略，只记录不阻止）
        if (storedUa != null && !storedUa.equals(currentUserAgent)) {
            log.info("Token 刷新 User-Agent 变化：jti={}, old={}, new={}", jti, storedUa, currentUserAgent);
            // 更新 User-Agent
            redisTemplate.opsForValue().set(RedisKeyConstants.tokenContextUa(jti), currentUserAgent,
                    securityProperties.getRefreshIntervalSeconds() * 2L, TimeUnit.SECONDS);
        }

        log.debug("Token 刷新上下文验证通过：jti={}, ip={}", jti, currentIp);
    }

    /**
     * 处理 IP 变化
     * <p>根据配置的验证模式采取不同策略</p>
     */
    private void handleIpChange(String jti, String storedIp, String currentIp, String refreshToken) {
        var mode = securityProperties.getTokenContextValidationMode();

        switch (mode) {
            case STRICT -> {
                // 严格模式：IP 变化直接拒绝
                log.warn("Token 刷新 IP 不一致（严格模式）：stored={}, current={}, jti={}",
                        storedIp, currentIp, jti);
                tokenManager.addToBlacklist(refreshToken);
                throw new BusinessException(ErrorCode.TOKEN_STOLEN);
            }
            case MODERATE -> {
                // 中等模式：短时间内多次 IP 变化才拒绝
                handleModerateMode(jti, storedIp, currentIp, refreshToken);
            }
            case LOOSE -> {
                // 宽松模式：仅记录日志，更新 IP
                log.info("Token 刷新 IP 变化（宽松模式）：stored={}, current={}, jti={}",
                        storedIp, currentIp, jti);
                updateTokenIp(jti, currentIp);
            }
        }
    }

    /**
     * 中等模式处理：短时间内多次 IP 变化才视为异常
     */
    private void handleModerateMode(String jti, String storedIp, String currentIp, String refreshToken) {
        String ipChangeKey = RedisKeyConstants.tokenIpChangeCount(jti);
        int threshold = securityProperties.getIpChangeThreshold();
        int windowMinutes = securityProperties.getIpChangeWindowMinutes();

        // 增加并获取 IP 变化次数
        Long changeCount = redisTemplate.opsForValue().increment(ipChangeKey);

        if (changeCount != null && changeCount == 1) {
            // 首次变化，设置过期时间
            redisTemplate.expire(ipChangeKey, windowMinutes, TimeUnit.MINUTES);
        }

        // 更新 IP
        updateTokenIp(jti, currentIp);

        if (changeCount != null && changeCount >= threshold) {
            // IP 变化次数超限，视为异常
            log.warn("Token 刷新 IP 变化次数超限（中等模式）：jti={}, count={}, threshold={}, stored={}, current={}",
                    jti, changeCount, threshold, storedIp, currentIp);
            tokenManager.addToBlacklist(refreshToken);
            throw new BusinessException(ErrorCode.TOKEN_STOLEN.getCode(),
                    "检测到异常的 IP 变化，请重新登录");
        }

        // 记录警告但允许继续
        log.warn("Token 刷新 IP 变化（中等模式）：jti={}, count={}, threshold={}, stored={}, current={}",
                jti, changeCount, threshold, storedIp, currentIp);
    }

    /**
     * 更新 Token 关联的 IP 地址
     */
    private void updateTokenIp(String jti, String newIp) {
        redisTemplate.opsForValue().set(RedisKeyConstants.tokenContextIp(jti), newIp,
                securityProperties.getRefreshIntervalSeconds() * 2L, TimeUnit.SECONDS);
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
        redisTemplate.opsForValue().set(RedisKeyConstants.tokenContextIp(jti), ip, expireSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(RedisKeyConstants.tokenContextUa(jti), ua, expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 检查 Token 刷新频率限制
     */
    private void checkRefreshFrequency(String refreshToken) {
        String username = tokenManager.getUsernameFromToken(refreshToken);
        String refreshKey = RedisKeyConstants.tokenRefreshFreq(username);
        String countKey = RedisKeyConstants.tokenRefreshCount(username);

        // 从配置获取刷新间隔（秒）
        int refreshIntervalSeconds = securityProperties.getRefreshIntervalSeconds();
        // 从配置获取每日最大刷新次数
        int maxRefreshCountPerDay = securityProperties.getMaxRefreshCountPerDay();

        // 检查是否在最小间隔时间内
        String lastRefreshTime = redisTemplate.opsForValue().get(refreshKey);
        if (lastRefreshTime != null) {
            long currentTime = System.currentTimeMillis();
            long lastTime = Long.parseLong(lastRefreshTime);

            if (currentTime - lastTime < refreshIntervalSeconds * 1000L) {
                log.warn("Token 刷新频率过高：username={}, 时间间隔={}ms", SensitiveUtils.maskUsername(username), currentTime - lastTime);
                throw new BusinessException(ErrorCode.TOKEN_REFRESH_TOO_FREQUENT);
            }
        }

        // 更新最后刷新时间
        redisTemplate.opsForValue().set(refreshKey, String.valueOf(System.currentTimeMillis()),
                refreshIntervalSeconds * 2L, TimeUnit.SECONDS);

        // 检查当日刷新次数
        String today = LocalDate.now().toString();
        String dailyCountKey = RedisKeyConstants.tokenRefreshDailyCount(username, today);

        Long currentCount = redisTemplate.opsForValue().increment(dailyCountKey);
        if (currentCount == 1) {
            // 设置过期时间为明天零点
            redisTemplate.expire(dailyCountKey, getSecondsUntilTomorrow(), TimeUnit.SECONDS);
        }

        if (currentCount > maxRefreshCountPerDay) {
            log.warn("Token 刷新次数超限：username={}, 当日次数={}", SensitiveUtils.maskUsername(username), currentCount);
            throw new BusinessException(ErrorCode.TOKEN_REFRESH_LIMIT_EXCEEDED);
        }
    }

    /**
     * 获取距离明天零点的秒数
     */
    private long getSecondsUntilTomorrow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1).withHour(0).withMinute(0).withSecond(0);
        return Duration.between(now, tomorrow).getSeconds();
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