package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.enums.CommonStatus;
import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.system.api.dto.RoleDTO;
import cn.swiftadmin.system.api.dto.RoleQueryDTO;
import cn.swiftadmin.system.api.entity.SysRole;
import cn.swiftadmin.system.api.entity.SysRoleMenu;
import cn.swiftadmin.system.api.service.PermissionPackageService;
import cn.swiftadmin.system.api.service.RoleService;
import cn.swiftadmin.system.api.vo.RoleVO;
import cn.swiftadmin.system.mapper.SysRoleMapper;
import cn.swiftadmin.system.mapper.SysRoleMenuMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysRoleTableDef.SYS_ROLE;
import static cn.swiftadmin.system.api.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;
import static cn.swiftadmin.system.api.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;

/**
 * 角色服务实现类
 * <p>支持自动生成角色编码，支持使用权限包快速分配菜单权限</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Service
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final PermissionPackageService permissionPackageService;

    public RoleServiceImpl(SysRoleMenuMapper roleMenuMapper,
                          @Lazy PermissionPackageService permissionPackageService) {
        this.roleMenuMapper = roleMenuMapper;
        this.permissionPackageService = permissionPackageService;
    }

    @Override
    public Page<RoleVO> page(RoleQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_ROLE.NAME.like(PageHelper.likeIfPresent(query.getName())))
                .and(SYS_ROLE.CODE.like(PageHelper.likeIfPresent(query.getCode())))
                .and(SYS_ROLE.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .orderBy(SYS_ROLE.CREATE_TIME.desc());

        return PageHelper.convert(
                getMapper().paginate(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper),
                role -> BeanCopyUtils.copy(role, RoleVO::new)
        );
    }

    @Override
    public RoleVO getDetail(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            return null;
        }
        RoleVO vo = BeanCopyUtils.copy(role, RoleVO::new);
        vo.setMenuIds(getMenuIdsByRoleId(id));
        return vo;
    }

    @Override
    public List<RoleVO> listAll() {
        return BeanCopyUtils.copyList(list(), RoleVO::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleDTO dto) {
        // 自动生成角色编码
        String code = dto.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = generateRoleCode(dto.getName());
        }

        if (existsByCode(code, null)) {
            throw new BusinessException(400, "角色编码已存在");
        }

        SysRole role = SysRole.builder()
                .name(dto.getName())
                .code(code)
                .description(dto.getDescription())
                .dataScope(dto.getDataScope() != null ? dto.getDataScope() : 5)
                .packageId(dto.getPackageId())
                .status(dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : CommonStatus.ENABLED)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();

        save(role);

        // 如果指定了权限包，使用权限包的菜单权限
        if (dto.getPackageId() != null) {
            permissionPackageService.applyToRole(role.getId(), dto.getPackageId());
        } else if (PageHelper.isNotEmpty(dto.getMenuIds())) {
            assignMenus(role.getId(), dto.getMenuIds());
        }

        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "角色 ID 不能为空");
        }

        SysRole existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "角色不存在");
        }

        // 自动生成角色编码（如果未提供）
        String code = dto.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = existing.getCode();
        }

        if (!existing.getCode().equals(code) && existsByCode(code, dto.getId())) {
            throw new BusinessException(400, "角色编码已存在");
        }

        UpdateChain.of(SysRole.class)
                .set(SYS_ROLE.NAME, dto.getName())
                .set(SYS_ROLE.CODE, code)
                .set(SYS_ROLE.DESCRIPTION, dto.getDescription())
                .set(SYS_ROLE.DATA_SCOPE, dto.getDataScope())
                .set(SYS_ROLE.PACKAGE_ID, dto.getPackageId())
                .set(SYS_ROLE.STATUS, dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : null)
                .set(SYS_ROLE.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_ROLE.ID.eq(dto.getId()))
                .update();

        // 如果指定了权限包，使用权限包的菜单权限
        if (dto.getPackageId() != null) {
            permissionPackageService.applyToRole(dto.getId(), dto.getPackageId());
        } else if (dto.getMenuIds() != null) {
            assignMenus(dto.getId(), dto.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (PageHelper.isNotEmpty(ids)) {
            getMapper().deleteBatchByIds(ids);
            roleMenuMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_MENU.ROLE_ID.in(ids)));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_MENU.ROLE_ID.eq(roleId)));

        if (PageHelper.isNotEmpty(menuIds)) {
            List<SysRoleMenu> roleMenus = menuIds.stream()
                    .map(menuId -> SysRoleMenu.builder().roleId(roleId).menuId(menuId).build())
                    .collect(Collectors.toList());
            roleMenuMapper.insertBatch(roleMenus);
        }
    }

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        return getMapper().selectListByQuery(
                QueryWrapper.create()
                        .select(SYS_ROLE.ALL_COLUMNS)
                        .from(SYS_ROLE)
                        .innerJoin(SYS_USER_ROLE).on(SYS_ROLE.ID.eq(SYS_USER_ROLE.ROLE_ID))
                        .where(SYS_USER_ROLE.USER_ID.eq(userId))
                        .and(SYS_ROLE.STATUS.eq(CommonStatus.ENABLED))
        );
    }

    @Override
    public boolean existsByCode(String code, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_ROLE.CODE.eq(code));

        if (excludeId != null) {
            queryWrapper.and(SYS_ROLE.ID.ne(excludeId));
        }

        return getMapper().selectCountByQuery(queryWrapper) > 0;
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectListByQuery(
                        QueryWrapper.create()
                                .select(SYS_ROLE_MENU.MENU_ID)
                                .from(SYS_ROLE_MENU)
                                .where(SYS_ROLE_MENU.ROLE_ID.eq(roleId)))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    /**
     * 自动生成角色编码
     * <p>规则：role_ + 名称简化 + 时间戳后4位</p>
     *
     * @param name 角色名称
     * @return 角色编码
     */
    private String generateRoleCode(String name) {
        String baseCode = "role_" + simplifyName(name);
        String code = baseCode;
        int suffix = 1;
        while (existsByCode(code, null)) {
            code = baseCode + "_" + suffix++;
        }
        return code;
    }

    /**
     * 简化名称用于生成编码
     */
    private String simplifyName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        // 简化处理：去除空格，转小写
        String simplified = name.replaceAll("\\s+", "_").toLowerCase();
        // 去除特殊字符，只保留字母数字下划线
        simplified = simplified.replaceAll("[^a-z0-9_]", "");
        if (simplified.isEmpty()) {
            return "unknown";
        }
        if (simplified.length() > 20) {
            return simplified.substring(0, 20);
        }
        return simplified;
    }

}