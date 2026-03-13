package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.enums.CommonStatus;
import cn.swiftadmin.common.core.enums.MenuType;
import cn.swiftadmin.system.api.dto.MenuDTO;
import cn.swiftadmin.system.api.entity.SysMenu;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.MenuService;
import cn.swiftadmin.system.api.service.PermissionCacheRefreshService;
import cn.swiftadmin.system.api.service.UserService;
import cn.swiftadmin.system.api.vo.MenuTreeVO;
import cn.swiftadmin.system.api.vo.MenuVO;
import cn.swiftadmin.system.mapper.SysMenuMapper;
import cn.swiftadmin.system.mapper.SysRoleMenuMapper;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysMenuTableDef.SYS_MENU;
import static cn.swiftadmin.system.api.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;
import static cn.swiftadmin.system.api.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;
import static com.mybatisflex.core.query.QueryMethods.distinct;

/**
 * 菜单服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements MenuService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final UserService userService;
    private final PermissionCacheRefreshService cacheRefreshService;

    @Override
    public List<MenuVO> getTree(Integer status) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_MENU.STATUS.eq(status, status != null))
                .orderBy(SYS_MENU.SORT_ORDER.asc(), SYS_MENU.CREATE_TIME.asc());

        return buildTree(list(queryWrapper), 0L);
    }

    @Override
    public MenuVO getDetail(Long id) {
        SysMenu menu = getById(id);
        return menu != null ? toMenuVO(menu) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MenuDTO dto) {
        SysMenu menu = SysMenu.builder()
                .parentId(dto.getParentId() != null ? dto.getParentId() : 0L)
                .name(dto.getName())
                .path(dto.getPath())
                .component(dto.getComponent())
                .type(MenuType.fromCode(dto.getType()))
                .icon(dto.getIcon())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .permission(dto.getPermission())
                .status(dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : CommonStatus.ENABLED)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();

        save(menu);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MenuDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "菜单 ID 不能为空");
        }

        if (dto.getParentId() != null && dto.getParentId().equals(dto.getId())) {
            throw new BusinessException(400, "父菜单不能是自己");
        }

        // 检查权限标识是否变更
        SysMenu existing = getById(dto.getId());
        boolean permissionChanged = existing != null &&
                !java.util.Objects.equals(existing.getPermission(), dto.getPermission());

        UpdateChain.of(SysMenu.class)
                .set(SYS_MENU.PARENT_ID, dto.getParentId() != null ? dto.getParentId() : 0L)
                .set(SYS_MENU.NAME, dto.getName())
                .set(SYS_MENU.PATH, dto.getPath())
                .set(SYS_MENU.COMPONENT, dto.getComponent())
                .set(SYS_MENU.TYPE, MenuType.fromCode(dto.getType()))
                .set(SYS_MENU.ICON, dto.getIcon())
                .set(SYS_MENU.SORT_ORDER, dto.getSortOrder())
                .set(SYS_MENU.PERMISSION, dto.getPermission())
                .set(SYS_MENU.STATUS, dto.getStatus() != null ? CommonStatus.fromCode(dto.getStatus()) : null)
                .set(SYS_MENU.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_MENU.ID.eq(dto.getId()))
                .update();

        // 如果权限标识变更，清除相关用户缓存
        if (permissionChanged) {
            cacheRefreshService.clearCacheByMenuId(dto.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (count(QueryWrapper.create().where(SYS_MENU.PARENT_ID.eq(id))) > 0) {
            throw new BusinessException(400, "存在子菜单，不能删除");
        }

        // 清除关联用户的权限缓存
        cacheRefreshService.clearCacheByMenuId(id);

        removeById(id);
        roleMenuMapper.deleteByQuery(QueryWrapper.create().where(SYS_ROLE_MENU.MENU_ID.eq(id)));
    }

    @Override
    public List<MenuTreeVO> getSelectTree() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_MENU.STATUS.eq(CommonStatus.ENABLED))
                .orderBy(SYS_MENU.SORT_ORDER.asc());

        return buildSelectTree(list(queryWrapper), 0L);
    }

    @Override
    public List<MenuVO> getUserMenu(Long userId) {
        return buildTree(getMenusByUserId(userId), 0L);
    }

    @Override
    public List<SysMenu> getMenusByUserId(Long userId) {
        // 超级管理员返回所有启用的菜单
        SysUser user = userService.getById(userId);
        if (user != null && user.getIsSuper() != null && user.getIsSuper().isYes()) {
            return list(QueryWrapper.create()
                    .where(SYS_MENU.STATUS.eq(CommonStatus.ENABLED))
                    .orderBy(SYS_MENU.SORT_ORDER.asc()));
        }

        // 普通用户根据角色查询菜单
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(distinct(SYS_MENU.ALL_COLUMNS))
                .from(SYS_MENU)
                .innerJoin(SYS_ROLE_MENU).on(SYS_MENU.ID.eq(SYS_ROLE_MENU.MENU_ID))
                .innerJoin(SYS_USER_ROLE).on(SYS_ROLE_MENU.ROLE_ID.eq(SYS_USER_ROLE.ROLE_ID))
                .where(SYS_USER_ROLE.USER_ID.eq(userId))
                .and(SYS_MENU.STATUS.eq(1))
                .orderBy(SYS_MENU.SORT_ORDER.asc());

        return mapper.selectListByQuery(queryWrapper);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return mapper.selectListByQueryAs(
                QueryWrapper.create()
                        .select(SYS_ROLE_MENU.MENU_ID)
                        .from(SYS_ROLE_MENU)
                        .where(SYS_ROLE_MENU.ROLE_ID.eq(roleId)),
                Long.class);
    }

    /**
     * 构建菜单树（优化版：O(n) 时间复杂度）
     */
    private List<MenuVO> buildTree(List<SysMenu> menus, Long rootParentId) {
        if (CollectionUtils.isEmpty(menus)) {
            return new ArrayList<>();
        }

        // 先转换为 VO 列表
        List<MenuVO> allVos = BeanCopyUtils.copyList(menus, MenuVO::new);

        // 按 parentId 分组
        Map<Long, List<MenuVO>> parentMap = allVos.stream()
                .collect(Collectors.groupingBy(vo -> vo.getParentId() != null ? vo.getParentId() : 0L));

        // 设置每个节点的 children
        allVos.forEach(vo -> vo.setChildren(parentMap.get(vo.getId())));

        // 返回根节点列表
        return parentMap.getOrDefault(rootParentId, new ArrayList<>());
    }

    /**
     * 构建选择树（优化版：O(n) 时间复杂度）
     */
    private List<MenuTreeVO> buildSelectTree(List<SysMenu> menus, Long rootParentId) {
        if (CollectionUtils.isEmpty(menus)) {
            return new ArrayList<>();
        }

        // 先转换为 VO 列表
        List<MenuTreeVO> allVos = menus.stream()
                .map(menu -> MenuTreeVO.of(menu.getId(), menu.getParentId(), menu.getName(),
                        menu.getType() != null ? menu.getType().getCode() : null))
                .collect(Collectors.toList());

        // 按 parentId 分组
        Map<Long, List<MenuTreeVO>> parentMap = allVos.stream()
                .collect(Collectors.groupingBy(vo -> vo.getParentId() != null ? vo.getParentId() : 0L));

        // 设置每个节点的 children
        allVos.forEach(vo -> vo.setChildren(parentMap.get(vo.getId())));

        // 返回根节点列表
        return parentMap.getOrDefault(rootParentId, new ArrayList<>());
    }

    /**
     * 转换为 MenuVO
     */
    private MenuVO toMenuVO(SysMenu menu) {
        return BeanCopyUtils.copy(menu, MenuVO::new);
    }

}