package cn.swiftadmin.auth.controller;

import cn.swiftadmin.auth.api.entity.SysLoginLog;
import cn.swiftadmin.auth.api.service.LoginLogService;
import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录日志控制器
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "登录日志管理", description = "系统登录日志查询接口")
@RestController
@RequestMapping("/api/auth/loginlog")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志", description = "支持按用户名、IP 地址、状态、时间范围筛选")
    @GetMapping("/page")
    @RequiresPermission("system:loginlog:list")
    public R<Page<SysLoginLog>> page(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(loginLogService.page(username, ip, status, beginTime, endTime, page, pageSize));
    }

    @Operation(summary = "获取登录日志详情")
    @GetMapping("/{id}")
    @RequiresPermission("system:loginlog:query")
    public R<SysLoginLog> getDetail(@PathVariable Long id) {
        return R.ok(loginLogService.getById(id));
    }

    @Operation(summary = "批量删除登录日志")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:loginlog:delete")
    public R<Void> delete(@PathVariable List<Long> ids) {
        loginLogService.deleteByIds(ids);
        return R.ok();
    }

    @Operation(summary = "清空所有登录日志")
    @DeleteMapping("/clear")
    @RequiresPermission("system:loginlog:delete")
    public R<Void> clear() {
        loginLogService.clearAll();
        return R.ok();
    }

}
