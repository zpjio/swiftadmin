package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.enums.CommonStatus;
import cn.swiftadmin.common.core.enums.YesNo;
import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.PasswordStrengthResult;
import cn.swiftadmin.common.core.service.PasswordValidator;
import cn.swiftadmin.common.mybatis.annotation.DataScope;
import cn.swiftadmin.common.mybatis.annotation.DataScopeValidate;
import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.system.api.dto.UserDTO;
import cn.swiftadmin.system.api.dto.UserQueryDTO;
import cn.swiftadmin.system.api.entity.SysRole;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.entity.SysUserRole;
import cn.swiftadmin.system.api.service.UserManageService;
import cn.swiftadmin.system.api.service.UserService;
import cn.swiftadmin.system.api.vo.UserVO;
import cn.swiftadmin.system.mapper.SysUserMapper;
import cn.swiftadmin.system.mapper.SysUserRoleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysUserTableDef.SYS_USER;
import static cn.swiftadmin.system.api.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;
import static cn.swiftadmin.system.api.entity.table.SysRoleTableDef.SYS_ROLE;
import static cn.swiftadmin.system.api.entity.table.SysDeptTableDef.SYS_DEPT;

/**
 * 用户管理服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
public class UserManageServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserManageService {

    private final SysUserRoleMapper userRoleMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    public UserManageServiceImpl(SysUserRoleMapper userRoleMapper, UserService userService,
                                  PasswordEncoder passwordEncoder, PasswordValidator passwordValidator) {
        this.userRoleMapper = userRoleMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
    }

    @Override
    @DataScope(userAlias = "u")
    public Page<UserVO> page(UserQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SYS_USER.ALL_COLUMNS, SYS_DEPT.NAME.as(UserVO.FIELD_DEPT_NAME))
                .from(SYS_USER.as("u"))
                .leftJoin(SYS_DEPT.as("d")).on(SYS_USER.DEPT_ID.eq(SYS_DEPT.ID))
                .where(SYS_USER.USERNAME.like(PageHelper.likeIfPresent(query.getUsername())))
                .and(SYS_USER.NICKNAME.like(PageHelper.likeIfPresent(query.getNickname())))
                .and(SYS_USER.PHONE.like(PageHelper.likeIfPresent(query.getPhone())))
                .and(SYS_USER.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .and(SYS_USER.DEPT_ID.eq(query.getDeptId(), query.getDeptId() != null))
                .and(SYS_USER.CREATE_TIME.ge(query.getCreateTimeBegin(), query.getCreateTimeBegin() != null))
                .and(SYS_USER.CREATE_TIME.le(query.getCreateTimeEnd(), query.getCreateTimeEnd() != null))
                .orderBy(SYS_USER.CREATE_TIME.desc());

        // 数据权限由 @DataScope 注解自动处理，无需手动调用

        return mapper
                .paginateAs(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper, UserVO.class);
    }

    @Override
    public UserVO getDetail(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            return null;
        }

        UserVO vo = BeanCopyUtils.copy(user, UserVO::new);

        // 获取用户角色
        List<SysRole> roles = mapper.selectListByQueryAs(
                QueryWrapper.create()
                        .select(SYS_ROLE.ID, SYS_ROLE.NAME)
                        .from(SYS_ROLE)
                        .innerJoin(SYS_USER_ROLE).on(SYS_ROLE.ID.eq(SYS_USER_ROLE.ROLE_ID))
                        .where(SYS_USER_ROLE.USER_ID.eq(id)),
                SysRole.class);

        vo.setRoleIds(roles.stream().map(SysRole::getId).collect(Collectors.toList()));
        vo.setRoleNames(roles.stream().map(SysRole::getName).collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserDTO dto) {
        if (userService.existsByUsername(dto.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 验证密码强度
        PasswordStrengthResult strengthResult = passwordValidator.validate(dto.getPassword(), dto.getUsername());
        if (!strengthResult.isValid()) {
            throw new BusinessException(400, strengthResult.getMessage());
        }

        SysUser user = SysUser.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .avatar(dto.getAvatar())
                .status(dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : CommonStatus.ENABLED)
                .deptId(dto.getDeptId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .loginFailCount(0)
                .passwordUpdateTime(LocalDateTime.now())
                .forceChangePassword(YesNo.NO)
                .build();

        save(user);

        if (!CollectionUtils.isEmpty(dto.getRoleIds())) {
            assignRoles(user.getId(), dto.getRoleIds());
        }

        return user.getId();
    }

    @Override
    @DataScopeValidate
    @Transactional(rollbackFor = Exception.class)
    public void update(UserDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "用户 ID 不能为空");
        }

        SysUser existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 检查超级管理员修改权限
        checkSuperAdminModifyPermission(dto.getId());

        // 检查用户名是否变更：仅当用户名发生变化且新用户名已存在时才报错
        String newUsername = dto.getUsername();
        if (newUsername != null && !newUsername.equals(existing.getUsername())) {
            if (userService.existsByUsername(newUsername)) {
                throw new BusinessException(400, "用户名已存在");
            }
        }

        // 判断是否需要更新敏感字段（跳过脱敏值）
        boolean shouldUpdatePhone = !isMaskedPhone(dto.getPhone());
        boolean shouldUpdateEmail = !isMaskedEmail(dto.getEmail());

        UpdateChain.of(SysUser.class)
                .set(SYS_USER.NICKNAME, dto.getNickname())
                .set(SYS_USER.EMAIL, shouldUpdateEmail ? dto.getEmail() : existing.getEmail())
                .set(SYS_USER.PHONE, shouldUpdatePhone ? dto.getPhone() : existing.getPhone())
                .set(SYS_USER.AVATAR, dto.getAvatar())
                .set(SYS_USER.STATUS, dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : null)
                .set(SYS_USER.DEPT_ID, dto.getDeptId())
                .set(SYS_USER.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_USER.ID.eq(dto.getId()))
                .update();

        if (dto.getRoleIds() != null) {
            assignRoles(dto.getId(), dto.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 检查是否包含超级管理员
        checkSuperAdminProtection(ids);

        mapper.deleteBatchByIds(ids);
        userRoleMapper.deleteByQuery(QueryWrapper.create().where(SYS_USER_ROLE.USER_ID.in(ids)));
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        // 检查是否为超级管理员
        checkSuperAdminProtection(userId);

        userService.updateStatus(userId, status);
    }

    @Override
    public void resetPassword(Long userId, String password) {
        userService.resetPassword(userId, password);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByQuery(QueryWrapper.create().where(SYS_USER_ROLE.USER_ID.eq(userId)));

        if (!CollectionUtils.isEmpty(roleIds)) {
            for (Long roleId : roleIds) {
                userRoleMapper.insertUserRole(userId, roleId);
            }
        }
    }

    // ==================== 超级管理员保护 ====================

    /**
     * 检查超级管理员保护（批量）
     * <p>超级管理员不可删除、不可禁用</p>
     *
     * @param userIds 用户ID列表
     * @throws BusinessException 如果包含超级管理员
     */
    private void checkSuperAdminProtection(List<Long> userIds) {
        for (Long userId : userIds) {
            checkSuperAdminProtection(userId);
        }
    }

    /**
     * 检查超级管理员保护（单个）
     *
     * @param userId 用户ID
     * @throws BusinessException 如果是超级管理员
     */
    private void checkSuperAdminProtection(Long userId) {
        SysUser user = getById(userId);
        if (user != null && user.getIsSuper() == YesNo.YES) {
            throw new BusinessException(403, "超级管理员[" + user.getUsername() + "]不可进行此操作");
        }
    }

    /**
     * 检查是否可以修改超级管理员
     * <p>只有超级管理员自己才能修改自己的信息</p>
     *
     * @param targetUserId 目标用户ID
     * @throws BusinessException 如果无权限修改
     */
    private void checkSuperAdminModifyPermission(Long targetUserId) {
        SysUser user = getById(targetUserId);
        if (user != null && user.getIsSuper() == YesNo.YES) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (!targetUserId.equals(currentUserId)) {
                throw new BusinessException(403, "只有超级管理员自己才能修改自己的信息");
            }
        }
    }

    // ==================== 脱敏值检测 ====================

    /** 手机号脱敏格式正则：如 138****0000 */
    private static final java.util.regex.Pattern MASKED_PHONE_PATTERN = java.util.regex.Pattern.compile("^\\d{3}\\*{4}\\d{4}$");

    /** 邮箱脱敏格式正则：如 a***@example.com */
    private static final java.util.regex.Pattern MASKED_EMAIL_PATTERN = java.util.regex.Pattern.compile("^.+\\*+.+@.+$");

    /**
     * 判断是否为脱敏后的手机号
     *
     * @param phone 手机号
     * @return true=脱敏值
     */
    private boolean isMaskedPhone(String phone) {
        return phone != null && MASKED_PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 判断是否为脱敏后的邮箱
     *
     * @param email 邮箱
     * @return true=脱敏值
     */
    private boolean isMaskedEmail(String email) {
        return email != null && MASKED_EMAIL_PATTERN.matcher(email).matches();
    }

}