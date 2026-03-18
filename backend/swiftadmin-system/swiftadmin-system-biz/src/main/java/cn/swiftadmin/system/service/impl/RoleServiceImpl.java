package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.enums.CommonStatus;
import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.system.api.dto.RoleDTO;
import cn.swiftadmin.system.api.dto.RoleQueryDTO;
import cn.swiftadmin.system.api.entity.SysRole;
import cn.swiftadmin.system.api.entity.SysRoleMenu;
import cn.swiftadmin.system.api.entity.SysRoleDept;
import cn.swiftadmin.system.api.service.PermissionCacheRefreshService;
import cn.swiftadmin.system.api.service.RoleService;
import cn.swiftadmin.system.api.vo.RoleVO;
import cn.swiftadmin.system.mapper.SysRoleMapper;
import cn.swiftadmin.system.mapper.SysRoleMenuMapper;
import cn.swiftadmin.system.mapper.SysRoleDeptMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysRoleTableDef.SYS_ROLE;
import static cn.swiftadmin.system.api.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;
import static cn.swiftadmin.system.api.entity.table.SysRoleDeptTableDef.SYS_ROLE_DEPT;
import static cn.swiftadmin.system.api.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;

/**
 * 角色服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Service
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final PermissionCacheRefreshService cacheRefreshService;

    public RoleServiceImpl(SysRoleMenuMapper roleMenuMapper,
                          SysRoleDeptMapper roleDeptMapper,
                          PermissionCacheRefreshService cacheRefreshService) {
        this.roleMenuMapper = roleMenuMapper;
        this.roleDeptMapper = roleDeptMapper;
        this.cacheRefreshService = cacheRefreshService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleVO> page(RoleQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_ROLE.NAME.like(PageHelper.likeIfPresent(query.getName())))
                .and(SYS_ROLE.CODE.like(PageHelper.likeIfPresent(query.getCode())))
                .and(SYS_ROLE.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .orderBy(SYS_ROLE.CREATE_TIME.desc());

        return mapper.paginateAs(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper, RoleVO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleVO getDetail(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            return null;
        }
        RoleVO vo = BeanCopyUtils.copy(role, RoleVO::new);
        vo.setMenuIds(getMenuIdsByRoleId(id));
        vo.setDeptIds(getDeptIdsByRoleId(id));
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
        if (!StringUtils.hasText(code)) {
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
                .status(dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : CommonStatus.ENABLED)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();

        save(role);

        // 分配菜单权限
        if (!CollectionUtils.isEmpty(dto.getMenuIds())) {
            assignMenus(role.getId(), dto.getMenuIds());
        }

        // 分配数据权限（自定义）
        if (dto.getDataScope() != null && dto.getDataScope() == 2 && !CollectionUtils.isEmpty(dto.getDeptIds())) {
            assignDepts(role.getId(), dto.getDeptIds());
        }

        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole update(RoleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "角色 ID 不能为空");
        }

        SysRole existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "角色不存在");
        }

        // 自动生成角色编码（如果未提供）
        String code = dto.getCode();
        if (!StringUtils.hasText(code)) {
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
                .set(SYS_ROLE.STATUS, dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : null)
                .set(SYS_ROLE.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_ROLE.ID.eq(dto.getId()))
                .update();

        // 清除拥有该角色的用户权限缓存
        cacheRefreshService.clearCacheByRoleId(dto.getId());

        // 更新菜单权限
        if (dto.getMenuIds() != null) {
            assignMenus(dto.getId(), dto.getMenuIds());
        }

        // 更新数据权限
        if (dto.getDataScope() != null) {
            if (dto.getDataScope() == 2) {
                // 自定义数据权限，无论 deptIds 是否为空都要更新（会清除旧的关联）
                assignDepts(dto.getId(), dto.getDeptIds() != null ? dto.getDeptIds() : List.of());
            } else {
                // 如果数据范围不是自定义，清除原有的部门关联
                roleDeptMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_DEPT.ROLE_ID.eq(dto.getId())));
            }
        }

        // 返回更新后的角色实体
        return getById(dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            // 清除拥有这些角色的用户权限缓存
            cacheRefreshService.clearCacheByRoleIds(ids);

            mapper.deleteBatchByIds(ids);
            roleMenuMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_MENU.ROLE_ID.in(ids)));
            roleDeptMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_DEPT.ROLE_ID.in(ids)));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_MENU.ROLE_ID.eq(roleId)));

        if (!CollectionUtils.isEmpty(menuIds)) {
            // 批量插入优化：构建实体列表，一次性插入
            List<SysRoleMenu> roleMenus = menuIds.stream()
                    .map(menuId -> SysRoleMenu.builder()
                            .roleId(roleId)
                            .menuId(menuId)
                            .build())
                    .collect(Collectors.toList());
            roleMenuMapper.insertBatch(roleMenus);
        }

        // 清除拥有该角色的用户权限缓存
        cacheRefreshService.clearCacheByRoleId(roleId);
    }

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        return mapper.selectListByQuery(
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

        return mapper.selectCountByQuery(queryWrapper) > 0;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDepts(Long roleId, List<Long> deptIds) {
        // 先删除原有的关联
        roleDeptMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_DEPT.ROLE_ID.eq(roleId)));

        // 保存新的关联
        if (!CollectionUtils.isEmpty(deptIds)) {
            // 批量插入优化：构建实体列表，一次性插入
            List<SysRoleDept> roleDepts = deptIds.stream()
                    .map(deptId -> SysRoleDept.builder()
                            .roleId(roleId)
                            .deptId(deptId)
                            .build())
                    .collect(Collectors.toList());
            roleDeptMapper.insertBatch(roleDepts);
        }

        // 清除拥有该角色的用户权限缓存
        cacheRefreshService.clearCacheByRoleId(roleId);
    }

    @Override
    public List<Long> getDeptIdsByRoleId(Long roleId) {
        return roleDeptMapper.selectListByQuery(
                        QueryWrapper.create()
                                .select(SYS_ROLE_DEPT.DEPT_ID)
                                .from(SYS_ROLE_DEPT)
                                .where(SYS_ROLE_DEPT.ROLE_ID.eq(roleId)))
                .stream()
                .map(SysRoleDept::getDeptId)
                .collect(Collectors.toList());
    }

    /**
     * 自动生成角色编码
     * <p>规则：role_ + 名称简化</p>
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
        if (!StringUtils.hasLength(name)) {
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