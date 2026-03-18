package cn.swiftadmin.system.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.system.api.dto.AssignRoleDTO;
import cn.swiftadmin.system.api.dto.ResetPasswordDTO;
import cn.swiftadmin.system.api.dto.UpdateStatusDTO;
import cn.swiftadmin.system.api.dto.UserDTO;
import cn.swiftadmin.system.api.dto.UserQueryDTO;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserManageService;
import cn.swiftadmin.system.api.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
    @OperationLog(title = "用户管理", businessType = OperationLog.BusinessType.INSERT,
            entityClass = SysUser.class, isSaveAfterData = true)
    public R<Long> create(@Validated(UserDTO.Create.class) @RequestBody UserDTO dto) {
        return R.ok(userManageService.create(dto));
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户", description = "修改用户信息")
    @PutMapping
    @RequiresPermission("system:user:edit")
    @OperationLog(title = "用户管理", businessType = OperationLog.BusinessType.UPDATE,
            entityClass = SysUser.class, isSaveBeforeData = true, isSaveAfterData = true)
    public R<Void> update(@Validated(UserDTO.Update.class) @RequestBody UserDTO dto) {
        userManageService.update(dto);
        return R.ok();
    }

    /**
     * 批量删除用户
     */
    @Operation(summary = "批量删除用户", description = "批量删除用户")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:user:delete")
    @OperationLog(title = "用户管理", businessType = OperationLog.BusinessType.DELETE,
            entityClass = SysUser.class, isSaveBeforeData = true)
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
    @OperationLog(title = "用户管理", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> updateStatus(@Valid @RequestBody UpdateStatusDTO dto) {
        userManageService.updateStatus(dto.getUserId(), dto.getStatus());
        return R.ok();
    }

    /**
     * 重置用户密码
     */
    @Operation(summary = "重置用户密码", description = "重置用户密码")
    @PutMapping("/reset-password")
    @RequiresPermission("system:user:resetPwd")
    @OperationLog(title = "用户管理", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userManageService.resetPassword(dto.getUserId(), dto.getPassword());
        return R.ok();
    }

    /**
     * 为用户分配角色
     */
    @Operation(summary = "分配角色", description = "为用户分配角色")
    @PutMapping("/role")
    @RequiresPermission("system:user:edit")
    @OperationLog(title = "用户管理", businessType = OperationLog.BusinessType.GRANT)
    public R<Void> assignRoles(@Valid @RequestBody AssignRoleDTO dto) {
        userManageService.assignRoles(dto.getUserId(), dto.getRoleIds());
        return R.ok();
    }

}