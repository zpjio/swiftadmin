package cn.swiftadmin.system.controller;

import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.system.api.dto.UserDTO;
import cn.swiftadmin.system.api.dto.UserQueryDTO;
import cn.swiftadmin.system.api.service.UserManageService;
import cn.swiftadmin.system.api.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "用户管理", description = "用户信息管理接口")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserManageService userManageService;

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "分页查询用户", description = "分页查询用户列表")
    @GetMapping("/page")
    @RequiresPermission("system:user:list")
    public R<Page<UserVO>> page(UserQueryDTO query) {
        return R.ok(userManageService.page(query));
    }

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息")
    @GetMapping("/current")
    public R<UserVO> getCurrentUser() {
        return R.ok(userManageService.getDetail(SecurityUtils.getCurrentUserId()));
    }

    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "根据用户 ID 获取详细信息")
    @GetMapping("/{id}")
    @RequiresPermission("system:user:query")
    public R<UserVO> getDetail(@PathVariable Long id) {
        return R.ok(userManageService.getDetail(id));
    }

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "新增系统用户")
    @PostMapping
    @RequiresPermission("system:user:add")
    public R<Long> create(@Valid @RequestBody UserDTO dto) {
        return R.ok(userManageService.create(dto));
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户", description = "修改用户信息")
    @PutMapping
    @RequiresPermission("system:user:edit")
    public R<Void> update(@Valid @RequestBody UserDTO dto) {
        userManageService.update(dto);
        return R.ok();
    }

    /**
     * 批量删除用户
     */
    @Operation(summary = "批量删除用户", description = "批量删除用户")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:user:delete")
    public R<Void> delete(@PathVariable List<Long> ids) {
        userManageService.delete(ids);
        return R.ok();
    }

    /**
     * 更新用户状态
     */
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    @PutMapping("/status")
    @RequiresPermission("system:user:edit")
    public R<Void> updateStatus(@RequestParam Long userId, @RequestParam Integer status) {
        userManageService.updateStatus(userId, status);
        return R.ok();
    }

    /**
     * 重置用户密码
     */
    @Operation(summary = "重置用户密码", description = "重置用户密码")
    @PutMapping("/reset-password")
    @RequiresPermission("system:user:resetPwd")
    public R<Void> resetPassword(@RequestParam Long userId, @RequestParam String password) {
        userManageService.resetPassword(userId, password);
        return R.ok();
    }

    /**
     * 为用户分配角色
     */
    @Operation(summary = "分配角色", description = "为用户分配角色")
    @PutMapping("/role")
    @RequiresPermission("system:user:edit")
    public R<Void> assignRoles(@RequestParam Long userId, @RequestParam List<Long> roleIds) {
        userManageService.assignRoles(userId, roleIds);
        return R.ok();
    }

}