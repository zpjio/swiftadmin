package cn.swiftadmin.system.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.dto.PermissionPackageDTO;
import cn.swiftadmin.system.api.dto.PermissionPackageQueryDTO;
import cn.swiftadmin.system.api.service.PermissionPackageService;
import cn.swiftadmin.system.api.vo.PermissionPackageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限包管理控制器
 * <p>提供权限包的管理功能</p>
 * <p>权限包是权限的集合，可快速为角色分配一组权限</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "权限包管理", description = "权限包管理接口")
@RestController
@RequestMapping("/api/system/permission-package")
@RequiredArgsConstructor
public class PermissionPackageController {

    private final PermissionPackageService permissionPackageService;

    /**
     * 分页查询权限包
     */
    @Operation(summary = "分页查询权限包", description = "支持按名称、状态等条件筛选")
    @GetMapping("/page")
    @RequiresPermission("system:permissionPackage:list")
    public R<Page<PermissionPackageVO>> page(PermissionPackageQueryDTO query) {
        return R.ok(permissionPackageService.page(query));
    }

    /**
     * 获取权限包详情
     */
    @GetMapping("/{id}")
    @RequiresPermission("system:permissionPackage:query")
    public R<PermissionPackageVO> getDetail(@PathVariable Long id) {
        return R.ok(permissionPackageService.getDetail(id));
    }

    /**
     * 获取所有权限包列表
     */
    @GetMapping("/list")
    @RequiresPermission("system:permissionPackage:list")
    public R<List<PermissionPackageVO>> listAll() {
        return R.ok(permissionPackageService.listAll());
    }

    /**
     * 创建权限包
     */
    @PostMapping
    @RequiresPermission("system:permissionPackage:add")
    public R<Long> create(@Valid @RequestBody PermissionPackageDTO dto) {
        return R.ok(permissionPackageService.create(dto));
    }

    /**
     * 更新权限包
     */
    @Operation(summary = "更新权限包", description = "修改权限包信息和权限列表")
    @PutMapping
    @RequiresPermission("system:permissionPackage:edit")
    public R<Void> update(@Valid @RequestBody PermissionPackageDTO dto) {
        permissionPackageService.update(dto);
        return R.ok();
    }

    /**
     * 批量删除权限包
     */
    @Operation(summary = "批量删除权限包", description = "支持同时删除多个权限包")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:permissionPackage:delete")
    public R<Void> delete(@PathVariable List<Long> ids) {
        permissionPackageService.delete(ids);
        return R.ok();
    }

    /**
     * 应用权限包到角色
     */
    @Operation(summary = "应用权限包到角色", description = "将权限包的权限快速分配给指定角色")
    @PostMapping("/apply")
    @RequiresPermission("system:role:edit")
    public R<Void> applyToRole(@RequestParam Long roleId, @RequestParam Long packageId) {
        permissionPackageService.applyToRole(roleId, packageId);
        return R.ok();
    }

}