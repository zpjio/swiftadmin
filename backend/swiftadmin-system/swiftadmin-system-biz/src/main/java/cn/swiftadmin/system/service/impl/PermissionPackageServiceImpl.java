package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.system.api.dto.PermissionPackageDTO;
import cn.swiftadmin.system.api.dto.PermissionPackageQueryDTO;
import cn.swiftadmin.system.api.entity.SysPermissionPackage;
import cn.swiftadmin.system.api.entity.SysPermissionPackageMenu;
import cn.swiftadmin.system.api.service.PermissionPackageService;
import cn.swiftadmin.system.api.service.RoleService;
import cn.swiftadmin.system.api.vo.PermissionPackageVO;
import cn.swiftadmin.system.mapper.SysPermissionPackageMapper;
import cn.swiftadmin.system.mapper.SysPermissionPackageMenuMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysPermissionPackageTableDef.SYS_PERMISSION_PACKAGE;
import static cn.swiftadmin.system.api.entity.table.SysPermissionPackageMenuTableDef.SYS_PERMISSION_PACKAGE_MENU;

/**
 * 权限包服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Service
public class PermissionPackageServiceImpl extends ServiceImpl<SysPermissionPackageMapper, SysPermissionPackage> implements PermissionPackageService {

    private final SysPermissionPackageMenuMapper packageMenuMapper;
    private final RoleService roleService;

    public PermissionPackageServiceImpl(SysPermissionPackageMenuMapper packageMenuMapper,
                                        RoleService roleService) {
        this.packageMenuMapper = packageMenuMapper;
        this.roleService = roleService;
    }

    @Override
    public Page<PermissionPackageVO> page(PermissionPackageQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_PERMISSION_PACKAGE.NAME.like(PageHelper.likeIfPresent(query.getName())))
                .and(SYS_PERMISSION_PACKAGE.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .orderBy(SYS_PERMISSION_PACKAGE.SORT_ORDER.asc(), SYS_PERMISSION_PACKAGE.CREATE_TIME.desc());

        return getMapper().paginateAs(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper, PermissionPackageVO.class);
    }

    @Override
    public PermissionPackageVO getDetail(Long id) {
        SysPermissionPackage pkg = getById(id);
        if (pkg == null) {
            return null;
        }
        PermissionPackageVO vo = toPermissionPackageVO(pkg);
        vo.setMenuIds(getMenuIdsByPackageId(id));
        return vo;
    }

    @Override
    public List<PermissionPackageVO> listAll() {
        return list(QueryWrapper.create()
                        .where(SYS_PERMISSION_PACKAGE.STATUS.eq(1))
                        .orderBy(SYS_PERMISSION_PACKAGE.SORT_ORDER.asc()))
                .stream()
                .map(this::toPermissionPackageVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PermissionPackageDTO dto) {
        // 自动生成编码
        String code = dto.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = generateCode(dto.getName());
        }

        if (existsByCode(code, null)) {
            throw new BusinessException(400, "权限包编码已存在");
        }

        SysPermissionPackage pkg = SysPermissionPackage.builder()
                .name(dto.getName())
                .code(code)
                .description(dto.getDescription())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();

        save(pkg);

        if (!CollectionUtils.isEmpty(dto.getMenuIds())) {
            assignMenus(pkg.getId(), dto.getMenuIds());
        }

        return pkg.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PermissionPackageDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "权限包 ID 不能为空");
        }

        SysPermissionPackage existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "权限包不存在");
        }

        String code = dto.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = existing.getCode();
        }

        if (!existing.getCode().equals(code) && existsByCode(code, dto.getId())) {
            throw new BusinessException(400, "权限包编码已存在");
        }

        UpdateChain.of(SysPermissionPackage.class)
                .set(SYS_PERMISSION_PACKAGE.NAME, dto.getName())
                .set(SYS_PERMISSION_PACKAGE.CODE, code)
                .set(SYS_PERMISSION_PACKAGE.DESCRIPTION, dto.getDescription())
                .set(SYS_PERMISSION_PACKAGE.SORT_ORDER, dto.getSortOrder())
                .set(SYS_PERMISSION_PACKAGE.STATUS, dto.getStatus())
                .set(SYS_PERMISSION_PACKAGE.UPDATE_TIME, LocalDateTime.now())
                .where(SYS_PERMISSION_PACKAGE.ID.eq(dto.getId()))
                .update();

        if (dto.getMenuIds() != null) {
            assignMenus(dto.getId(), dto.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            getMapper().deleteBatchByIds(ids);
            packageMenuMapper.deleteByQuery(QueryWrapper.create()
                    .where(SYS_PERMISSION_PACKAGE_MENU.PACKAGE_ID.in(ids)));
        }
    }

    @Override
    public List<Long> getMenuIdsByPackageId(Long packageId) {
        return packageMenuMapper.selectListByQuery(
                        QueryWrapper.create()
                                .select(SYS_PERMISSION_PACKAGE_MENU.MENU_ID)
                                .from(SYS_PERMISSION_PACKAGE_MENU)
                                .where(SYS_PERMISSION_PACKAGE_MENU.PACKAGE_ID.eq(packageId)))
                .stream()
                .map(SysPermissionPackageMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyToRole(Long roleId, Long packageId) {
        List<Long> menuIds = getMenuIdsByPackageId(packageId);
        if (!CollectionUtils.isEmpty(menuIds)) {
            roleService.assignMenus(roleId, menuIds);
        }
    }

    @Override
    public boolean existsByCode(String code, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_PERMISSION_PACKAGE.CODE.eq(code));

        if (excludeId != null) {
            queryWrapper.and(SYS_PERMISSION_PACKAGE.ID.ne(excludeId));
        }

        return getMapper().selectCountByQuery(queryWrapper) > 0;
    }

    /**
     * 分配菜单到权限包
     */
    private void assignMenus(Long packageId, List<Long> menuIds) {
        packageMenuMapper.deleteByQuery(QueryWrapper.create()
                .where(SYS_PERMISSION_PACKAGE_MENU.PACKAGE_ID.eq(packageId)));

        if (!CollectionUtils.isEmpty(menuIds)) {
            List<SysPermissionPackageMenu> packageMenus = menuIds.stream()
                    .map(menuId -> SysPermissionPackageMenu.builder()
                            .packageId(packageId)
                            .menuId(menuId)
                            .build())
                    .collect(Collectors.toList());
            packageMenuMapper.insertBatch(packageMenus);
        }
    }

    /**
     * 自动生成编码
     * <p>规则：pkg_ + 名称拼音首字母 + 时间戳后4位</p>
     */
    private String generateCode(String name) {
        String baseCode = "pkg_" + extractPinyin(name);
        String code = baseCode;
        int suffix = 1;
        while (existsByCode(code, null)) {
            code = baseCode + "_" + suffix++;
        }
        return code;
    }

    /**
     * 提取名称的简化编码（取首字母或简化处理）
     */
    private String extractPinyin(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        // 简化处理：去除空格，转小写，取前缀
        String simplified = name.replaceAll("\\s+", "_").toLowerCase();
        if (simplified.length() > 20) {
            return simplified.substring(0, 20);
        }
        return simplified;
    }

    private PermissionPackageVO toPermissionPackageVO(SysPermissionPackage pkg) {
        PermissionPackageVO vo = BeanCopyUtils.copy(pkg, PermissionPackageVO::new);
        // 查询菜单数量
        Long count = packageMenuMapper.selectCountByQuery(
                QueryWrapper.create().where(SYS_PERMISSION_PACKAGE_MENU.PACKAGE_ID.eq(pkg.getId())));
        vo.setMenuCount(count != null ? count.intValue() : 0);
        return vo;
    }

}