package cn.swiftadmin.system.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.dto.RoleDTO;
import cn.swiftadmin.system.api.dto.RoleQueryDTO;
import cn.swiftadmin.system.api.entity.SysRole;
import cn.swiftadmin.system.api.service.RoleService;
import cn.swiftadmin.system.api.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "角色管理", description = "系统角色管理接口")
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色列表
     */
    @Operation(summary = "分页查询角色", description = "分页查询角色列表")
    @GetMapping("/page")
    @RequiresPermission("system:role:list")
    public R<Page<RoleVO>> page(RoleQueryDTO query) {
        return R.ok(roleService.page(query));
    }

    /**
     * 获取角色详情
     */
    @Operation(summary = "获取角色详情", description = "根据角色 ID 获取详细信息")
    @GetMapping("/{id}")
    @RequiresPermission("system:role:query")
    public R<RoleVO> getDetail(@PathVariable Long id) {
        return R.ok(roleService.getDetail(id));
    }

    /**
     * 获取角色列表（下拉选择）
     */
    @Operation(summary = "获取角色列表", description = "获取所有角色列表，用于下拉选择")
    @GetMapping("/list")
    @RequiresPermission("system:role:list")
    public R<List<RoleVO>> listAll() {
        return R.ok(roleService.listAll());
    }

    /**
     * 创建角色
     */
    @Operation(summary = "创建角色", description = "新增系统角色")
    @PostMapping
    @RequiresPermission("system:role:add")
    @OperationLog(title = "角色管理", businessType = OperationLog.BusinessType.INSERT,
            entityClass = SysRole.class, isSaveAfterData = true)
    public R<Long> create(@Valid @RequestBody RoleDTO dto) {
        return R.ok(roleService.create(dto));
    }

    /**
     * 更新角色信息
     */
    @Operation(summary = "更新角色", description = "修改角色信息")
    @PutMapping
    @RequiresPermission("system:role:edit")
    @OperationLog(title = "角色管理", businessType = OperationLog.BusinessType.UPDATE,
            entityClass = SysRole.class, isSaveBeforeData = true, isSaveAfterData = true)
    public R<SysRole> update(@Valid @RequestBody RoleDTO dto) {
        return R.ok(roleService.update(dto));
    }

    /**
     * 批量删除角色
     */
    @Operation(summary = "批量删除角色", description = "批量删除角色")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:role:delete")
    @OperationLog(title = "角色管理", businessType = OperationLog.BusinessType.DELETE,
            entityClass = SysRole.class, isSaveBeforeData = true)
    public R<Void> delete(@PathVariable List<Long> ids) {
        roleService.delete(ids);
        return R.ok();
    }

    /**
     * 为角色分配菜单权限
     */
    @Operation(summary = "分配菜单权限", description = "为角色分配菜单权限")
    @PutMapping("/menu")
    @RequiresPermission("system:role:edit")
    @OperationLog(title = "角色管理", businessType = OperationLog.BusinessType.GRANT)
    public R<Void> assignMenus(@RequestParam Long roleId, @RequestParam List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds);
        return R.ok();
    }

}