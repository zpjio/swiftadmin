package cn.swiftadmin.auth.service.impl;

import cn.swiftadmin.auth.api.dto.SocialUserInfo;
import cn.swiftadmin.auth.api.entity.SysSocialBind;
import cn.swiftadmin.auth.api.exception.SocialAuthError;
import cn.swiftadmin.auth.api.exception.SocialAuthException;
import cn.swiftadmin.auth.api.service.SocialBindService;
import cn.swiftadmin.auth.mapper.SysSocialBindMapper;
import cn.swiftadmin.common.core.util.AesUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cn.swiftadmin.auth.api.entity.table.SysSocialBindTableDef.SYS_SOCIAL_BIND;

/**
 * 社交账号绑定服务实现类
 * <p>继承 ServiceImpl 获得通用 CRUD 能力</p>
 * <p>使用 UpdateChain 优化更新操作，避免先查询再更新</p>
 * <p>Token 加密方案：</p>
 * <ul>
 *   <li>实体操作：SetListener 自动加密</li>
 *   <li>UpdateChain：需手动加密（SetListener 可能不触发）</li>
 *   <li>读取操作：TypeHandler 自动解密</li>
 * </ul>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
public class SocialBindServiceImpl extends ServiceImpl<SysSocialBindMapper, SysSocialBind> implements SocialBindService {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    /** Token 加密密钥 */
    @Value("${social.token-secret:swiftadmin-social-token-secret-key}")
    private String tokenSecret;

    /** 分布式锁超时时间（秒） */
    private static final int LOCK_TIMEOUT_SECONDS = 30;

    public SocialBindServiceImpl(
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SysSocialBind getBySourceAndOpenId(String source, String openId) {
        return getOne(QueryWrapper.create()
                .where(SYS_SOCIAL_BIND.SOURCE.eq(source))
                .and(SYS_SOCIAL_BIND.OPEN_ID.eq(openId))
                .limit(1));
    }

    @Override
    public SysSocialBind getByUserIdAndSource(Long userId, String source) {
        return getOne(QueryWrapper.create()
                .where(SYS_SOCIAL_BIND.USER_ID.eq(userId))
                .and(SYS_SOCIAL_BIND.SOURCE.eq(source))
                .limit(1));
    }

    @Override
    public List<SysSocialBind> getByUserId(Long userId) {
        return list(QueryWrapper.create()
                .where(SYS_SOCIAL_BIND.USER_ID.eq(userId))
                .orderBy(SYS_SOCIAL_BIND.BIND_TIME, false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bind(Long userId, String source, String openId, Map<String, Object> userInfo) {
        // 转换为标准 DTO
        SocialUserInfo socialUserInfo = userInfo instanceof Map ?
                SocialUserInfo.fromMap((Map<String, Object>) userInfo) :
                SocialUserInfo.fromMap(userInfo);

        // 使用分布式锁防止并发绑定
        String lockKey = "social:bind:lock:" + source.toLowerCase() + ":" + openId;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, String.valueOf(userId), LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("绑定操作被锁定：userId={}, source={}, openId={}", userId, source, openId);
            throw new SocialAuthException(SocialAuthError.OPERATION_IN_PROGRESS);
        }

        try {
            doBind(userId, source.toLowerCase(), openId, socialUserInfo);
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 执行绑定操作（内部方法）
     * <p>AesGcmTypeHandler 会在 save() 时自动加密 accessToken 和 refreshToken</p>
     */
    private void doBind(Long userId, String source, String openId, SocialUserInfo userInfo) {
        // 检查用户是否已绑定同来源账号
        SysSocialBind userBind = getByUserIdAndSource(userId, source);
        if (userBind != null) {
            throw new SocialAuthException(SocialAuthError.USER_ALREADY_BOUND);
        }

        // 创建绑定关系（依赖数据库唯一索引防止重复绑定）
        // AesGcmTypeHandler 会在 save() 时自动加密 accessToken 和 refreshToken
        SysSocialBind bind = SysSocialBind.builder()
                .userId(userId)
                .source(source)
                .openId(openId)
                .unionId(userInfo.getUnionId())
                .accessToken(userInfo.getAccessToken())
                .refreshToken(userInfo.getRefreshToken())
                .tokenExpiresAt(calculateTokenExpiresAt(userInfo.getExpiresIn()))
                .extraInfo(serializeExtraInfo(userInfo))
                .bindTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        try {
            save(bind);
            log.info("用户绑定社交账号成功：userId={}, source={}, openId={}", userId, source, openId);
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突，说明该社交账号已被其他用户绑定
            log.warn("社交账号已被绑定：source={}, openId={}", source, openId);
            throw new SocialAuthException(SocialAuthError.BIND_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long userId, String source) {
        SysSocialBind bind = getByUserIdAndSource(userId, source.toLowerCase());
        if (bind == null) {
            throw new SocialAuthException(SocialAuthError.BIND_NOT_FOUND);
        }

        removeById(bind.getId());
        log.info("用户解绑社交账号：userId={}, source={}", userId, source);
    }

    /**
     * 更新 Token（使用 UpdateChain 直接更新，一条 SQL 完成）
     * <p>注意：UpdateChain 可能不触发 SetListener，需手动加密</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateToken(Long bindId, Map<String, Object> userInfo) {
        // 检查是否存在
        if (getById(bindId) == null) {
            throw new SocialAuthException(SocialAuthError.BIND_NOT_FOUND);
        }

        SocialUserInfo socialUserInfo = SocialUserInfo.fromMap(userInfo);

        String accessToken = socialUserInfo.getAccessToken();
        String refreshToken = socialUserInfo.getRefreshToken();
        LocalDateTime tokenExpiresAt = calculateTokenExpiresAt(socialUserInfo.getExpiresIn());

        // 使用 UpdateChain 一次性更新，避免先查询再更新
        // UpdateChain 可能不触发 SetListener，需手动加密
        UpdateChain.of(SysSocialBind.class)
                .set(SYS_SOCIAL_BIND.ACCESS_TOKEN, encryptToken(accessToken), accessToken != null)
                .set(SYS_SOCIAL_BIND.REFRESH_TOKEN, encryptToken(refreshToken), refreshToken != null)
                .set(SYS_SOCIAL_BIND.TOKEN_EXPIRES_AT, tokenExpiresAt, tokenExpiresAt != null)
                .set(SYS_SOCIAL_BIND.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_SOCIAL_BIND.ID.eq(bindId))
                .update();

        log.info("更新社交账号 Token：bindId={}", bindId);
    }

    @Override
    public boolean isBound(String source, String openId) {
        return exists(QueryWrapper.create()
                .where(SYS_SOCIAL_BIND.SOURCE.eq(source))
                .and(SYS_SOCIAL_BIND.OPEN_ID.eq(openId)));
    }

    /**
     * 加密 Token（用于 UpdateChain 操作）
     * <p>UpdateChain 可能不触发 SetListener，需手动加密</p>
     */
    private String encryptToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return AesUtils.encrypt(token, tokenSecret);
    }

    /**
     * 计算 Token 过期时间
     */
    private LocalDateTime calculateTokenExpiresAt(Long expiresIn) {
        if (expiresIn == null || expiresIn <= 0) {
            return null;
        }
        return LocalDateTime.now().plusSeconds(expiresIn);
    }

    /**
     * 序列化额外信息为 JSON
     */
    private String serializeExtraInfo(SocialUserInfo userInfo) {
        try {
            Map<String, Object> extraInfo = userInfo.toMap();
            // 移除敏感字段
            extraInfo.remove("accessToken");
            extraInfo.remove("refreshToken");
            extraInfo.remove("expiresIn");
            extraInfo.remove("source");
            return objectMapper.writeValueAsString(extraInfo);
        } catch (JsonProcessingException e) {
            log.warn("序列化额外信息失败", e);
            return null;
        }
    }

}