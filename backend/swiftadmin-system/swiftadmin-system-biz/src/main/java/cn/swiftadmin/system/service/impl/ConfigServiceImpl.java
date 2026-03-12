package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.system.api.dto.ConfigDTO;
import cn.swiftadmin.system.api.dto.ConfigQueryDTO;
import cn.swiftadmin.system.api.entity.SysConfig;
import cn.swiftadmin.system.api.service.ConfigService;
import cn.swiftadmin.system.api.util.DictUtils;
import cn.swiftadmin.system.api.vo.ConfigVO;
import cn.swiftadmin.system.mapper.SysConfigMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cn.swiftadmin.system.api.entity.table.SysConfigTableDef.SYS_CONFIG;

/**
 * 系统配置服务实现类
 * <p>使用 Redis 缓存配置值，减少数据库访问</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
public class ConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ConfigService {

    private final StringRedisTemplate redisTemplate;

    /** 配置缓存前缀 */
    private static final String CACHE_PREFIX = "swiftadmin:config:";

    /** 缓存过期时间（小时） */
    private static final int CACHE_EXPIRE_HOURS = 24;

    /** 配置类型字典 */
    private static final String CONFIG_TYPE_DICT = "sys_config_type";

    public ConfigServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== 配置读取方法 ====================

    @Override
    public String getValue(String key, String defaultValue) {
        // 1. 先从 Redis 缓存获取
        String cacheKey = CACHE_PREFIX + key;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 2. 缓存未命中，从数据库查询
        SysConfig config = getOne(QueryWrapper.create()
                .where(SYS_CONFIG.CONFIG_KEY.eq(key)));

        String value = config != null ? config.getConfigValue() : defaultValue;

        // 3. 写入缓存
        if (value != null) {
            redisTemplate.opsForValue().set(cacheKey, value, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }

        return value;
    }

    @Override
    public int getIntValue(String key, int defaultValue) {
        String value = getValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolValue(String key, boolean defaultValue) {
        String value = getValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    // ==================== 配置管理方法 ====================

    @Override
    public Page<ConfigVO> page(ConfigQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_CONFIG.CONFIG_KEY.like(PageHelper.likeIfPresent(query.getConfigKey())))
                .and(SYS_CONFIG.CONFIG_NAME.like(PageHelper.likeIfPresent(query.getConfigName())))
                .and(SYS_CONFIG.CONFIG_TYPE.eq(query.getConfigType(), query.getConfigType() != null))
                .orderBy(SYS_CONFIG.CONFIG_KEY.asc());

        return PageHelper.convert(
                getMapper().paginate(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper),
                config -> BeanCopyUtils.copy(config, ConfigVO::new)
        );
    }

    @Override
    public ConfigVO getDetail(Long id) {
        SysConfig config = getById(id);
        return config != null ? BeanCopyUtils.copy(config, ConfigVO::new) : null;
    }

    @Override
    public ConfigVO getByKey(String configKey) {
        SysConfig config = getOne(QueryWrapper.create()
                .where(SYS_CONFIG.CONFIG_KEY.eq(configKey)));
        return config != null ? BeanCopyUtils.copy(config, ConfigVO::new) : null;
    }

    @Override
    public List<ConfigVO> listByType(String configType) {
        return BeanCopyUtils.copyList(
                list(QueryWrapper.create()
                        .where(SYS_CONFIG.CONFIG_TYPE.eq(configType))
                        .orderBy(SYS_CONFIG.CONFIG_KEY.asc())),
                ConfigVO::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ConfigDTO dto) {
        if (existsByKey(dto.getConfigKey(), null)) {
            throw new BusinessException(400, "配置键已存在");
        }

        // 校验配置类型
        String configType = dto.getConfigType() != null ? dto.getConfigType() : "system";
        if (!isValidConfigType(configType)) {
            throw new BusinessException(400, "无效的配置类型: " + configType);
        }

        SysConfig config = SysConfig.builder()
                .configKey(dto.getConfigKey())
                .configValue(dto.getConfigValue())
                .configName(dto.getConfigName())
                .configType(configType)
                .remark(dto.getRemark())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        save(config);

        // 写入缓存
        cacheValue(config.getConfigKey(), config.getConfigValue());

        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ConfigDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "配置 ID 不能为空");
        }

        SysConfig existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "配置不存在");
        }

        // 如果修改了配置键，检查新键是否已存在
        if (!existing.getConfigKey().equals(dto.getConfigKey()) && existsByKey(dto.getConfigKey(), dto.getId())) {
            throw new BusinessException(400, "配置键已存在");
        }

        // 校验配置类型
        if (dto.getConfigType() != null && !isValidConfigType(dto.getConfigType())) {
            throw new BusinessException(400, "无效的配置类型: " + dto.getConfigType());
        }

        UpdateChain.of(SysConfig.class)
                .set(SYS_CONFIG.CONFIG_KEY, dto.getConfigKey())
                .set(SYS_CONFIG.CONFIG_VALUE, dto.getConfigValue())
                .set(SYS_CONFIG.CONFIG_NAME, dto.getConfigName())
                .set(SYS_CONFIG.CONFIG_TYPE, dto.getConfigType())
                .set(SYS_CONFIG.REMARK, dto.getRemark())
                .set(SYS_CONFIG.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_CONFIG.ID.eq(dto.getId()))
                .update();

        // 清除旧缓存，写入新缓存
        evictCache(existing.getConfigKey());
        cacheValue(dto.getConfigKey(), dto.getConfigValue());

        log.info("配置已更新: {} = {}", dto.getConfigKey(), dto.getConfigValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 获取要删除的配置键，清除缓存
        List<SysConfig> configs = listByIds(ids);
        configs.forEach(config -> evictCache(config.getConfigKey()));

        // 删除数据库记录
        getMapper().deleteBatchByIds(ids);

        log.info("配置已删除: ids={}", ids);
    }

    @Override
    public void refreshCache(String configKey) {
        if (configKey != null && !configKey.isEmpty()) {
            // 刷新指定配置的缓存
            evictCache(configKey);
            SysConfig config = getOne(QueryWrapper.create()
                    .where(SYS_CONFIG.CONFIG_KEY.eq(configKey)));
            if (config != null) {
                cacheValue(config.getConfigKey(), config.getConfigValue());
            }
            log.info("配置缓存已刷新: {}", configKey);
        } else {
            // 刷新全部缓存
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.info("全部配置缓存已刷新");
        }
    }

    @Override
    public boolean existsByKey(String configKey, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_CONFIG.CONFIG_KEY.eq(configKey));

        if (excludeId != null) {
            queryWrapper.and(SYS_CONFIG.ID.ne(excludeId));
        }

        return getMapper().selectCountByQuery(queryWrapper) > 0;
    }

    // ==================== 私有方法 ====================

    /**
     * 写入缓存
     */
    private void cacheValue(String key, String value) {
        if (value != null) {
            String cacheKey = CACHE_PREFIX + key;
            redisTemplate.opsForValue().set(cacheKey, value, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
    }

    /**
     * 清除缓存
     */
    private void evictCache(String key) {
        String cacheKey = CACHE_PREFIX + key;
        redisTemplate.delete(cacheKey);
    }

    /**
     * 校验配置类型是否有效
     *
     * @param configType 配置类型
     * @return 有效返回 true
     */
    private boolean isValidConfigType(String configType) {
        if (configType == null || configType.isEmpty()) {
            return false;
        }
        return DictUtils.existsValue(CONFIG_TYPE_DICT, configType);
    }

}