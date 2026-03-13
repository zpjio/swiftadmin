package cn.swiftadmin.common.security.jwt;

import cn.swiftadmin.common.core.constants.RedisKeyConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token 提供者
 * <p>负责生成、解析和验证 JWT Token，使用 HS256 算法</p>
 * <p>支持 Token 黑名单机制，实现登出功能</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final StringRedisTemplate redisTemplate;
    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final long expiration;
    private final long refreshExpiration;

    public JwtTokenProvider(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:7200000}") long expiration,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpiration) {
        this.redisTemplate = redisTemplate;
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;

        // 验证密钥长度（HS256 要求至少 256 位 = 32 字节）
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密钥不能为空，请在配置文件中设置 jwt.secret");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT 密钥长度不足，HS256 算法要求至少 32 字节（256 位），当前长度：" + keyBytes.length);
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        // 预创建 JwtParser（线程安全，可复用）
        this.jwtParser = Jwts.parser()
                .verifyWith(this.secretKey)
                .build();
        
        log.info("JWT Token Provider 初始化成功，密钥长度：{} 字节", keyBytes.length);
    }

    /**
     * 获取访问 Token 有效期（毫秒）
     *
     * @return 有效期（毫秒）
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * 获取刷新 Token 有效期（毫秒）
     *
     * @return 有效期（毫秒）
     */
    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    /**
     * 生成访问 Token（有效期 2 小时）
     *
     * @param username 用户名
     * @return JWT Token
     */
    public String generateToken(String username) {
        Date now = new Date();
        String jti = UUID.randomUUID().toString().replace("-", "");

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .claim("type", "access")
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成刷新 Token（有效期 7 天）
     *
     * @param username 用户名
     * @return 刷新 Token
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        String jti = UUID.randomUUID().toString().replace("-", "");

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .claim("type", "refresh")
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成临时 Token（用于强制修改密码等场景，有效期 10 分钟，只能使用 1 次）
     *
     * @param username 用户名
     * @return 临时 Token
     */
    public String generateTempToken(String username) {
        Date now = new Date();
        String jti = UUID.randomUUID().toString().replace("-", "");
        long tempExpiration = TimeUnit.MINUTES.toMillis(10);  // 10 分钟有效期

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + tempExpiration))
                .claim("type", "temp")
                .claim("maxUses", 1)  // 最多使用 1 次
                .claim("usedCount", 0)  // 已使用次数
                .signWith(secretKey)
                .compact();
    }

    /**
     * 检查是否为临时 Token
     *
     * @param token JWT Token
     * @return true=临时 Token
     */
    public boolean isTempToken(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return "temp".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为刷新 Token
     *
     * @param token JWT Token
     * @return true=刷新 Token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为访问 Token
     *
     * @param token JWT Token
     * @return true=访问 Token
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return "access".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查临时 Token 是否可以使用（一次性使用）
     * <p>使用 Redis 记录临时 Token 的使用状态</p>
     *
     * @param token JWT Token
     * @return true=可以使用
     */
    public boolean canUseTempToken(String token) {
        try {
            if (!isTempToken(token)) {
                return false;
            }
            
            // 获取 jti 作为 Redis key
            String jti = getJtiFromToken(token);

            // 检查是否已使用
            Boolean isUsed = redisTemplate.hasKey(RedisKeyConstants.tempTokenUsed(jti));
            if (Boolean.TRUE.equals(isUsed)) {
                log.warn("临时 Token 已被使用：jti={}", jti);
                return false;
            }

            // 标记为已使用（设置过期时间 10 分钟，与 Token 有效期一致）
            redisTemplate.opsForValue().set(RedisKeyConstants.tempTokenUsed(jti), "used", 10, TimeUnit.MINUTES);
            return true;
        } catch (Exception e) {
            log.error("检查临时 Token 使用状态失败", e);
            return false;
        }
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    /**
     * 从 Token 中获取 JWT ID (jti)
     *
     * @param token JWT Token
     * @return JWT ID
     */
    public String getJtiFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.getId();
    }

    /**
     * 获取 Token 剩余有效期（毫秒）
     *
     * @param token JWT Token
     * @return 剩余有效期（毫秒），已过期返回 0
     */
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return true=有效，false=无效或过期
     */
    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.debug("Token 验证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param token JWT Token
     * @return true=在黑名单中，false=不在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        String jti = getJtiFromToken(token);
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeyConstants.jwtBlacklist(jti)));
    }

    /**
     * 将 Token 加入黑名单
     *
     * @param token JWT Token
     */
    public void addToBlacklist(String token) {
        String jti = getJtiFromToken(token);
        if (jti == null) {
            return;
        }
        long remainingTime = getTokenRemainingTime(token);
        if (remainingTime > 0) {
            redisTemplate.opsForValue().set(RedisKeyConstants.jwtBlacklist(jti), "1", remainingTime, TimeUnit.MILLISECONDS);
            log.debug("Token 已加入黑名单: jti={}, 剩余有效时间: {}ms", jti, remainingTime);
        }
    }

}
