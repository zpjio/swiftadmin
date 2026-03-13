package cn.swiftadmin.common.core.service;

import cn.swiftadmin.common.core.result.PasswordStrengthResult;

/**
 * 密码验证服务接口
 * <p>提供密码复杂度校验功能，支持可配置的密码策略</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface PasswordValidator {

    /**
     * 验证密码强度
     *
     * @param password 密码
     * @return 验证结果
     */
    PasswordStrengthResult validate(String password);

    /**
     * 验证密码强度（包含用户名检查）
     *
     * @param password 密码
     * @param username 用户名
     * @return 验证结果
     */
    PasswordStrengthResult validate(String password, String username);

    /**
     * 验证密码强度（包含历史密码检查）
     *
     * @param password 密码
     * @param username 用户名
     * @param historyPasswords 历史密码列表
     * @return 验证结果
     */
    PasswordStrengthResult validate(String password, String username, String... historyPasswords);

    /**
     * 快速校验密码是否满足策略要求
     *
     * @param password 密码
     * @return true=满足要求
     */
    boolean isValid(String password);

    /**
     * 快速校验密码是否满足策略要求
     *
     * @param password 密码
     * @param username 用户名
     * @return true=满足要求
     */
    boolean isValid(String password, String username);

    /**
     * 计算密码强度分数（0-100）
     *
     * @param password 密码
     * @return 强度分数
     */
    int calculateStrengthScore(String password);

    /**
     * 获取密码强度等级
     *
     * @param password 密码
     * @return 强度等级：WEAK|MEDIUM|STRONG
     */
    String getStrengthLevel(String password);
}