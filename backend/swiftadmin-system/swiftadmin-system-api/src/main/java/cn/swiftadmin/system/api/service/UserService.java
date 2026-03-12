package cn.swiftadmin.system.api.service;

import cn.swiftadmin.system.api.entity.SysUser;
import com.mybatisflex.core.service.IService;

/**
 * 用户服务接口
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
public interface UserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息，不存在返回 null
     */
    SysUser getByUsername(String username);

    /**
     * 重置密码
     *
     * @param userId 用户 ID
     * @param newPassword 新密码（明文，会自动加密）
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 更新用户状态
     *
     * @param userId 用户 ID
     * @param status 状态：1=正常，0=禁用
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return true=存在，false=不存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息，不存在返回 null
     */
    SysUser getByPhone(String phone);

    /**
     * 更新用户社交信息（昵称、头像）
     *
     * @param userId 用户 ID
     * @param nickname 昵称
     * @param avatar 头像 URL
     */
    void updateSocialInfo(Long userId, String nickname, String avatar);

    /**
     * 增加登录失败次数
     *
     * @param username 用户名
     * @return 当前失败次数
     */
    int incrementLoginFailCount(String username);

    /**
     * 重置登录失败次数
     *
     * @param username 用户名
     */
    void resetLoginFailCount(String username);

    /**
     * 锁定账户
     *
     * @param username 用户名
     * @param lockMinutes 锁定分钟数
     */
    void lockAccount(String username, int lockMinutes);

    /**
     * 检查账户是否被锁定
     *
     * @param username 用户名
     * @return true=已锁定，false=未锁定
     */
    boolean isAccountLocked(String username);

    /**
     * 修改密码（带密码强度校验）
     *
     * @param userId 用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return true=修改成功
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 强制修改密码（无需旧密码验证）
     *
     * @param userId 用户 ID
     * @param newPassword 新密码
     * @return true=修改成功
     */
    boolean forceChangePassword(Long userId, String newPassword);

    /**
     * 检查密码是否过期
     *
     * @param user 用户信息
     * @return true=已过期
     */
    boolean isPasswordExpired(SysUser user);

    /**
     * 获取密码剩余有效天数
     *
     * @param user 用户信息
     * @return 剩余天数，0表示已过期
     */
    int getPasswordRemainingDays(SysUser user);

}
