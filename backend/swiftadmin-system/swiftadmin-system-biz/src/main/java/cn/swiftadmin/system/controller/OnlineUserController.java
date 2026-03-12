package cn.swiftadmin.system.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.service.OnlineUserService;
import cn.swiftadmin.system.api.vo.OnlineUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 在线用户控制器
 * <p>提供在线用户的监控和管理功能</p>
 * <p>支持查看在线用户列表、强制下线等操作</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "在线用户管理", description = "在线用户监控和管理接口")
@RestController
@RequestMapping("/api/system/online")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    /**
     * 分页查询在线用户
     * <p>支持按用户名、IP 地址等条件筛选</p>
     *
     * @param username 用户名（可选）
     * @param ip IP 地址（可选）
     * @param page 页码（默认 1）
     * @param pageSize 每页数量（默认 10）
     * @return 在线用户分页列表
     */
    @Operation(summary = "分页查询在线用户", description = "支持按用户名、IP 地址等条件筛选")
    @GetMapping("/page")
    @RequiresPermission("monitor:online:list")
    public R<Page<OnlineUserVO>> page(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String ip,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(onlineUserService.page(username, ip, page, pageSize));
    }

    /**
     * 获取所有在线用户列表
     * <p>返回当前所有在线用户的完整信息</p>
     *
     * @return 在线用户列表
     */
    @Operation(summary = "获取所有在线用户列表", description = "返回当前所有在线用户的完整信息")
    @GetMapping("/list")
    @RequiresPermission("monitor:online:list")
    public R<List<OnlineUserVO>> listAll() {
        return R.ok(onlineUserService.listAll());
    }

    /**
     * 获取在线用户数量
     * <p>统计当前在线用户总数</p>
     *
     * @return 在线用户数量
     */
    @Operation(summary = "获取在线用户数量", description = "统计当前在线用户总数")
    @GetMapping("/count")
    @RequiresPermission("monitor:online:list")
    public R<Long> count() {
        return R.ok(onlineUserService.count());
    }

    /**
     * 强制用户下线
     * <p>立即终止指定用户的登录会话</p>
     *
     * @param sessionId 会话 ID
     * @return 操作结果
     */
    @Operation(summary = "强制用户下线", description = "立即终止指定用户的登录会话")
    @DeleteMapping("/{sessionId}")
    @RequiresPermission("monitor:online:forceLogout")
    @OperationLog(title = "在线用户", businessType = OperationLog.BusinessType.FORCE)
    public R<Void> forceLogout(@PathVariable String sessionId) {
        onlineUserService.forceLogout(sessionId);
        return R.ok();
    }

    /**
     * 批量强制用户下线
     * <p>同时让多个用户下线，用于批量管理</p>
     *
     * @param sessionIds 会话 ID 列表
     * @return 成功下线的用户数量
     */
    @Operation(summary = "批量强制用户下线", description = "同时让多个用户下线，用于批量管理")
    @DeleteMapping("/batch")
    @RequiresPermission("monitor:online:forceLogout")
    @OperationLog(title = "在线用户", businessType = OperationLog.BusinessType.FORCE)
    public R<Integer> forceLogoutBatch(@RequestBody List<String> sessionIds) {
        return R.ok(onlineUserService.forceLogoutBatch(sessionIds));
    }

}