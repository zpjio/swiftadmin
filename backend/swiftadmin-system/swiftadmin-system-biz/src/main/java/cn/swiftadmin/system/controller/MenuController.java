package cn.swiftadmin.system.controller;

import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.system.api.dto.MenuDTO;
import cn.swiftadmin.system.api.service.MenuService;
import cn.swiftadmin.system.api.vo.MenuTreeVO;
import cn.swiftadmin.system.api.vo.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 * <p>提供菜单的增删改查功能</p>
 * <p>支持树形结构展示，用于系统菜单权限管理</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "菜单管理", description = "系统菜单权限管理接口")
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 获取菜单树形列表
     * <p>返回完整的菜单树形结构，支持按状态过滤</p>
     *
     * @param status 菜单状态（可选，0=禁用，1=启用）
     * @return 菜单树形列表
     */
    @Operation(summary = "获取菜单树", description = "返回完整的菜单树形结构，支持按状态过滤")
    @GetMapping("/tree")
    @RequiresPermission("system:menu:list")
    public R<List<MenuVO>> getTree(@RequestParam(required = false) Integer status) {
        return R.ok(menuService.getTree(status));
    }

    /**
     * 获取菜单详情
     * <p>根据菜单 ID 获取详细信息</p>
     *
     * @param id 菜单 ID
     * @return 菜单详情
     */
    @Operation(summary = "获取菜单详情", description = "根据菜单 ID 获取详细信息")
    @GetMapping("/{id}")
    @RequiresPermission("system:menu:query")
    public R<MenuVO> getDetail(@PathVariable Long id) {
        return R.ok(menuService.getDetail(id));
    }

    /**
     * 创建菜单
     * <p>支持创建目录、菜单、按钮三种类型</p>
     *
     * @param dto 菜单创建参数
     * @return 新创建的菜单 ID
     */
    @Operation(summary = "创建菜单", description = "支持创建目录、菜单、按钮三种类型")
    @PostMapping
    @RequiresPermission("system:menu:add")
    public R<Long> create(@Valid @RequestBody MenuDTO dto) {
        return R.ok(menuService.create(dto));
    }

    /**
     * 更新菜单信息
     * <p>支持修改菜单名称、路径、权限标识等</p>
     *
     * @param dto 菜单更新参数
     * @return 操作结果
     */
    @Operation(summary = "更新菜单", description = "支持修改菜单名称、路径、权限标识等")
    @PutMapping
    @RequiresPermission("system:menu:edit")
    public R<Void> update(@Valid @RequestBody MenuDTO dto) {
        menuService.update(dto);
        return R.ok();
    }

    /**
     * 删除菜单
     * <p>删除前会检查是否有子菜单，有子菜单时不允许删除</p>
     *
     * @param id 菜单 ID
     * @return 操作结果
     */
    @Operation(summary = "删除菜单", description = "删除菜单，有子菜单时不允许删除")
    @DeleteMapping("/{id}")
    @RequiresPermission("system:menu:delete")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }

    /**
     * 获取菜单下拉树（用于选择）
     * <p>返回简化的菜单树，用于编辑时选择父级菜单</p>
     *
     * @return 菜单下拉树
     */
    @Operation(summary = "获取菜单下拉树", description = "返回简化的菜单树，用于编辑时选择父级菜单")
    @GetMapping("/select-tree")
    @RequiresPermission("system:menu:list")
    public R<List<MenuTreeVO>> getSelectTree() {
        return R.ok(menuService.getSelectTree());
    }

    /**
     * 获取当前登录用户的菜单权限
     * <p>根据当前用户的角色权限返回可访问的菜单</p>
     * <p>超级管理员返回所有菜单</p>
     *
     * @return 当前用户的菜单列表
     */
    @Operation(summary = "获取当前用户菜单", description = "根据当前用户的角色权限返回可访问的菜单")
    @GetMapping("/user")
    public R<List<MenuVO>> getUserMenu() {
        return R.ok(menuService.getUserMenu(SecurityUtils.getCurrentUserId()));
    }

}