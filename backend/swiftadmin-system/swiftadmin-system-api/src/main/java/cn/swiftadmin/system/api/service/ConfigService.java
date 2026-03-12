package cn.swiftadmin.system.api.service;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.system.api.dto.ConfigDTO;
import cn.swiftadmin.system.api.dto.ConfigQueryDTO;
import cn.swiftadmin.system.api.vo.ConfigVO;

import java.util.List;

/**
 * 系统配置服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface ConfigService {

    // ==================== 配置读取方法 ====================

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getValue(String key, String defaultValue);

    /**
     * 获取整数配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    int getIntValue(String key, int defaultValue);

    /**
     * 获取布尔配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    boolean getBoolValue(String key, boolean defaultValue);

    // ==================== 配置管理方法 ====================

    /**
     * 分页查询配置
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<ConfigVO> page(ConfigQueryDTO query);

    /**
     * 获取配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
    ConfigVO getDetail(Long id);

    /**
     * 根据配置键获取配置
     *
     * @param configKey 配置键
     * @return 配置详情
     */
    ConfigVO getByKey(String configKey);

    /**
     * 获取指定类型的配置列表
     *
     * @param configType 配置类型
     * @return 配置列表
     */
    List<ConfigVO> listByType(String configType);

    /**
     * 创建配置
     *
     * @param dto 配置信息
     * @return 配置ID
     */
    Long create(ConfigDTO dto);

    /**
     * 更新配置
     *
     * @param dto 配置信息
     */
    void update(ConfigDTO dto);

    /**
     * 删除配置
     *
     * @param ids 配置ID列表
     */
    void delete(List<Long> ids);

    /**
     * 刷新配置缓存
     *
     * @param configKey 配置键（为空则刷新全部）
     */
    void refreshCache(String configKey);

    /**
     * 检查配置键是否存在
     *
     * @param configKey 配置键
     * @param excludeId 排除的ID
     * @return true=存在
     */
    boolean existsByKey(String configKey, Long excludeId);

    // ==================== 密码策略配置快捷方法 ====================

    /**
     * 获取密码有效期（天）
     */
    default int getPasswordExpireDays() {
        return getIntValue("password.expire_days", 90);
    }

    /**
     * 获取密码历史检查数量
     */
    default int getPasswordHistoryCount() {
        return getIntValue("password.history_count", 5);
    }

    /**
     * 获取管理员重置后是否强制修改
     */
    default boolean isForceChangeOnReset() {
        return getBoolValue("password.force_change_on_reset", true);
    }

    /**
     * 获取密码过期提前提醒天数
     */
    default int getPasswordExpireWarningDays() {
        return getIntValue("password.expire_warning_days", 7);
    }

    /**
     * 获取密码最小长度
     */
    default int getPasswordMinLength() {
        return getIntValue("password.min_length", 8);
    }

}