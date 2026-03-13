package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.enums.CommonStatus;
import cn.swiftadmin.common.core.enums.YesNo;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.PasswordStrengthResult;
import cn.swiftadmin.common.core.service.PasswordValidator;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.ConfigService;
import cn.swiftadmin.system.api.service.PasswordHistoryService;
import cn.swiftadmin.system.api.service.UserService;
import cn.swiftadmin.system.mapper.SysUserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import static cn.swiftadmin.system.api.entity.table.SysUserTableDef.SYS_USER;

/**
 * 用户服务实现类
 * <p>使用 MyBatis-Flex UpdateChain 优化更新操作，避免先查询再更新</p>
 * <p>使用 @LogicDelete 注解自动处理软删除查询条件</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService passwordHistoryService;
    private final ConfigService configService;
    private final PasswordValidator passwordValidator;

    /**
     * 根据用户名查询用户
     * <p>@LogicDelete 注解自动处理 deleted = 0 条件</p>
     */
    @Override
    public SysUser getByUsername(String username) {
        return getOne(QueryWrapper.create()
                .where(SYS_USER.USERNAME.eq(username)));
    }

    /**
     * 重置密码（管理员重置）
     * <p>根据配置决定是否强制用户下次登录修改密码</p>
     */
    @Override
    public void resetPassword(Long userId, String newPassword) {
        // 校验用户存在
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 校验密码强度
        validatePasswordStrength(newPassword, user.getUsername());

        // 检查密码历史
        checkPasswordHistory(userId, newPassword);

        // 检查是否需要强制修改
        boolean forceChange = configService.isForceChangeOnReset();

        // 加密密码
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 更新密码和策略字段
        UpdateChain.of(SysUser.class)
                .set(SYS_USER.PASSWORD, encodedPassword)
                .set(SYS_USER.PASSWORD_UPDATE_TIME, LocalDateTime.now())
                .set(SYS_USER.FORCE_CHANGE_PASSWORD, forceChange ? YesNo.YES : YesNo.NO)
                .set(SYS_USER.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_USER.ID.eq(userId))
                .update();

        // 记录密码历史
        passwordHistoryService.addHistory(userId, encodedPassword);
        passwordHistoryService.cleanOldHistory(userId, configService.getPasswordHistoryCount());
    }

    /**
     * 更新用户状态
     */
    @Override
    public void updateStatus(Long userId, Integer status) {
        UpdateChain.of(SysUser.class)
                .set(SYS_USER.STATUS, status != null ? CommonStatus.fromCode(status) : null)
                .where(SYS_USER.ID.eq(userId))
                .update();
    }

    /**
     * 检查用户名是否存在
     */
    @Override
    public boolean existsByUsername(String username) {
        return exists(QueryWrapper.create()
                .where(SYS_USER.USERNAME.eq(username)));
    }

    /**
     * 根据手机号查询用户
     */
    @Override
    public SysUser getByPhone(String phone) {
        return getOne(QueryWrapper.create()
                .where(SYS_USER.PHONE.eq(phone)));
    }

    /**
     * 更新用户社交信息（昵称、头像）
     */
    @Override
    public void updateSocialInfo(Long userId, String nickname, String avatar) {
        UpdateChain<SysUser> chain = UpdateChain.of(SysUser.class);
        boolean hasUpdate = false;

        if (StringUtils.hasText(nickname)) {
            chain.set(SYS_USER.NICKNAME, nickname);
            hasUpdate = true;
        }
        if (StringUtils.hasText(avatar)) {
            chain.set(SYS_USER.AVATAR, avatar);
            hasUpdate = true;
        }

        if (hasUpdate) {
            chain.where(SYS_USER.ID.eq(userId)).update();
        }
    }

    /**
     * 增加登录失败次数
     * <p>使用 UpdateChain + setRaw 直接 +1，一次数据库操作完成</p>
     */
    @Override
    public int incrementLoginFailCount(String username) {
        // 使用 SQL 表达式直接 +1，一次数据库操作完成
        UpdateChain.of(SysUser.class)
                .setRaw(SYS_USER.LOGIN_FAIL_COUNT, "login_fail_count + 1")
                .where(SYS_USER.USERNAME.eq(username))
                .update();

        // 返回更新后的值（需要查询）
        SysUser user = getByUsername(username);
        return user != null ? (user.getLoginFailCount() == null ? 1 : user.getLoginFailCount()) : 0;
    }

    /**
     * 重置登录失败次数
     */
    @Override
    public void resetLoginFailCount(String username) {
        UpdateChain.of(SysUser.class)
                .set(SYS_USER.LOGIN_FAIL_COUNT, 0)
                .set(SYS_USER.LOCK_TIME, (LocalDateTime) null)
                .where(SYS_USER.USERNAME.eq(username))
                .update();
    }

    /**
     * 锁定账户
     */
    @Override
    public void lockAccount(String username, int lockMinutes) {
        UpdateChain.of(SysUser.class)
                .set(SYS_USER.LOCK_TIME, LocalDateTime.now().plusMinutes(lockMinutes))
                .where(SYS_USER.USERNAME.eq(username))
                .update();
    }

    /**
     * 检查账户是否被锁定
     */
    @Override
    public boolean isAccountLocked(String username) {
        SysUser user = getOne(QueryWrapper.create()
                .select(SYS_USER.ID, SYS_USER.LOCK_TIME)
                .where(SYS_USER.USERNAME.eq(username)));

        if (user == null || user.getLockTime() == null) {
            return false;
        }
        // 如果锁定时间已过，自动解锁
        if (user.getLockTime().isBefore(LocalDateTime.now())) {
            resetLoginFailCount(username);
            return false;
        }
        return true;
    }

    /**
     * 修改密码（带密码强度校验和历史检查）
     */
    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        // 查询用户
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }

        // 校验新密码强度
        validatePasswordStrength(newPassword, user.getUsername());

        // 检查密码历史
        checkPasswordHistory(userId, newPassword);

        // 加密密码
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 更新密码和策略字段
        UpdateChain.of(SysUser.class)
                .set(SYS_USER.PASSWORD, encodedPassword)
                .set(SYS_USER.PASSWORD_UPDATE_TIME, LocalDateTime.now())
                .set(SYS_USER.FORCE_CHANGE_PASSWORD, YesNo.NO)
                .set(SYS_USER.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_USER.ID.eq(userId))
                .update();

        // 记录密码历史
        passwordHistoryService.addHistory(userId, encodedPassword);
        passwordHistoryService.cleanOldHistory(userId, configService.getPasswordHistoryCount());

        return true;
    }

    /**
     * 强制修改密码（无需旧密码验证，使用临时 token）
     */
    @Override
    public boolean forceChangePassword(Long userId, String newPassword) {
        // 查询用户
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 校验密码强度
        validatePasswordStrength(newPassword, user.getUsername());

        // 检查密码历史
        checkPasswordHistory(userId, newPassword);

        // 加密密码
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 更新密码和策略字段
        UpdateChain.of(SysUser.class)
                .set(SYS_USER.PASSWORD, encodedPassword)
                .set(SYS_USER.PASSWORD_UPDATE_TIME, LocalDateTime.now())
                .set(SYS_USER.FORCE_CHANGE_PASSWORD, YesNo.NO)
                .set(SYS_USER.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_USER.ID.eq(userId))
                .update();

        // 记录密码历史
        passwordHistoryService.addHistory(userId, encodedPassword);
        passwordHistoryService.cleanOldHistory(userId, configService.getPasswordHistoryCount());

        return true;
    }

    /**
     * 检查密码是否过期
     */
    @Override
    public boolean isPasswordExpired(SysUser user) {
        if (user.getPasswordUpdateTime() == null) {
            return true;
        }
        int expireDays = configService.getPasswordExpireDays();
        return user.getPasswordUpdateTime()
                .plusDays(expireDays)
                .isBefore(LocalDateTime.now());
    }

    /**
     * 获取密码剩余有效天数
     */
    @Override
    public int getPasswordRemainingDays(SysUser user) {
        if (user.getPasswordUpdateTime() == null) {
            return 0;
        }
        int expireDays = configService.getPasswordExpireDays();
        LocalDateTime expireTime = user.getPasswordUpdateTime().plusDays(expireDays);
        long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expireTime);
        return (int) Math.max(0, remainingDays);
    }

    /**
     * 校验密码强度（使用可配置的密码策略）
     */
    private void validatePasswordStrength(String password, String username) {
        PasswordStrengthResult result = passwordValidator.validate(password, username);
        if (!result.isValid()) {
            throw new BusinessException(400, result.getMessage());
        }
    }

    /**
     * 检查密码历史
     */
    private void checkPasswordHistory(Long userId, String newPassword) {
        int historyCount = configService.getPasswordHistoryCount();
        if (passwordHistoryService.isPasswordInHistory(userId, newPassword, historyCount)) {
            throw new BusinessException(400, "新密码不能与最近 " + historyCount + " 次密码相同");
        }
    }

}
