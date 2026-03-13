package cn.swiftadmin.auth.util;

import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * IP 限流工具类
 * <p>基于 Redis 实现 IP 维度的登录限流，防止暴力破解和 DDoS 攻击</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查 IP 登录限流
     * <p>单 IP 每小时最多登录 20 次，超过则锁定 30 分钟</p>
     *
     * @param ip IP 地址
     */
    public void checkIpLoginLimit(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }

        String key = RedisKeyConstants.ipLoginAttempts(ip);
        Long attempts = redisTemplate.opsForValue().increment(key);

        // 首次访问设置过期时间
        if (attempts == 1) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }

        // 检查是否超过阈值（每小时 20 次）
        if (attempts > 20) {
            log.warn("IP 登录次数超限：ip={}, 次数={}", ip, attempts);
            // 将 IP 加入黑名单 30 分钟
            String blacklistKey = RedisKeyConstants.ipLoginBlacklist(ip);
            redisTemplate.opsForValue().set(blacklistKey, "blocked", 30, TimeUnit.MINUTES);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        log.debug("IP 登录检查：ip={}, 次数={}", ip, attempts);
    }

    /**
     * 检查 IP 是否在黑名单中
     *
     * @param ip IP 地址
     * @return true=在黑名单中
     */
    public boolean isIpBlacklisted(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        String blacklistKey = RedisKeyConstants.ipLoginBlacklist(ip);
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    /**
     * 将 IP 加入黑名单
     *
     * @param ip IP 地址
     * @param minutes 锁定时间（分钟）
     */
    public void addToBlacklist(String ip, int minutes) {
        if (ip == null || ip.isEmpty()) {
            return;
        }

        String blacklistKey = RedisKeyConstants.ipLoginBlacklist(ip);
        redisTemplate.opsForValue().set(blacklistKey, "blocked", minutes, TimeUnit.MINUTES);
        log.warn("IP 已加入黑名单：ip={}, 锁定时间={}分钟", ip, minutes);
    }

    /**
     * 重置 IP 登录计数
     *
     * @param ip IP 地址
     */
    public void resetIpCount(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }

        String key = RedisKeyConstants.ipLoginAttempts(ip);
        redisTemplate.delete(key);
        log.debug("IP 登录计数已重置：ip={}", ip);
    }
}
