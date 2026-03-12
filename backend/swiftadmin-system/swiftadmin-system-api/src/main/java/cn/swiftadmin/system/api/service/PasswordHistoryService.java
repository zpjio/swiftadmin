package cn.swiftadmin.system.api.service;

import cn.swiftadmin.system.api.entity.SysPasswordHistory;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 密码历史服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface PasswordHistoryService extends IService<SysPasswordHistory> {

    /**
     * 获取用户最近的密码历史
     *
     * @param userId 用户 ID
     * @param limit 数量限制
     * @return 密码哈希列表
     */
    List<String> getRecentPasswordHashes(Long userId, int limit);

    /**
     * 添加密码历史记录
     *
     * @param userId 用户 ID
     * @param passwordHash 密码哈希
     */
    void addHistory(Long userId, String passwordHash);

    /**
     * 清理过期的密码历史（保留最近 N 条）
     *
     * @param userId 用户 ID
     * @param keepCount 保留数量
     */
    void cleanOldHistory(Long userId, int keepCount);

    /**
     * 检查密码是否在历史记录中
     *
     * @param userId 用户 ID
     * @param rawPassword 明文密码
     * @param checkCount 检查数量
     * @return true=密码已存在于历史记录
     */
    boolean isPasswordInHistory(Long userId, String rawPassword, int checkCount);

}