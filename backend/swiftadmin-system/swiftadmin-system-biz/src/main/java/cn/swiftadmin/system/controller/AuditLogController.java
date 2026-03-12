package cn.swiftadmin.system.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.dto.AuditLogQueryDTO;
import cn.swiftadmin.system.api.entity.SysAuditLog;
import cn.swiftadmin.system.api.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审计日志控制器
 * <p>提供数据变更审计日志的查询功能</p>
 * <p>记录所有数据的增删改操作，支持数据变更历史追溯</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "审计日志管理", description = "数据变更审计日志查询接口")
@RestController
@RequestMapping("/api/system/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 分页查询审计日志
     * <p>支持按业务模块、操作类型、操作人员、表名、时间范围等条件筛选</p>
     */
    @Operation(summary = "分页查询审计日志", description = "支持按业务模块、操作类型、操作人员、表名、时间范围等条件筛选")
    @GetMapping("/page")
    @RequiresPermission("system:auditLog:list")
    public R<Page<SysAuditLog>> page(AuditLogQueryDTO query) {
        return R.ok(auditLogService.page(query));
    }

    /**
     * 获取审计日志详情
     * <p>查看具体的审计日志信息，包括变更前后的数据</p>
     *
     * @param id 日志 ID
     * @return 审计日志详情
     */
    @Operation(summary = "获取审计日志详情", description = "查看具体的审计日志信息，包括变更前后的数据")
    @GetMapping("/{id}")
    @RequiresPermission("system:auditLog:query")
    public R<SysAuditLog> getDetail(@PathVariable Long id) {
        return R.ok(auditLogService.getDetail(id));
    }

    /**
     * 获取数据变更历史
     * <p>查询指定记录的所有变更历史，用于数据追溯</p>
     *
     * @param tableName 表名
     * @param recordId 记录 ID
     * @return 变更历史列表
     */
    @Operation(summary = "获取数据变更历史", description = "查询指定记录的所有变更历史，用于数据追溯")
    @GetMapping("/history")
    @RequiresPermission("system:auditLog:list")
    public R<List<SysAuditLog>> getRecordHistory(
            @RequestParam String tableName,
            @RequestParam String recordId) {
        return R.ok(auditLogService.getRecordHistory(tableName, recordId));
    }

    /**
     * 获取业务模块列表
     * <p>返回所有已记录的业务模块名称，用于筛选</p>
     *
     * @return 业务模块列表
     */
    @Operation(summary = "获取业务模块列表", description = "返回所有已记录的业务模块名称，用于筛选")
    @GetMapping("/modules")
    @RequiresPermission("system:auditLog:list")
    public R<List<String>> listModules() {
        return R.ok(auditLogService.listModules());
    }

    /**
     * 批量删除审计日志
     * <p>支持同时删除多条日志记录</p>
     *
     * @param ids 日志 ID 列表
     * @return 操作结果
     */
    @Operation(summary = "批量删除审计日志", description = "支持同时删除多条日志记录")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:auditLog:delete")
    public R<Void> delete(@PathVariable List<Long> ids) {
        auditLogService.deleteByIds(ids);
        return R.ok();
    }

    /**
     * 清理指定天数前的审计日志
     * <p>定期清理旧日志，释放存储空间</p>
     *
     * @param beforeDays 清理多少天前的日志（默认 90 天）
     * @return 操作结果
     */
    @Operation(summary = "清理审计日志", description = "定期清理指定天数前的旧日志")
    @DeleteMapping("/clear")
    @RequiresPermission("system:auditLog:delete")
    public R<Void> clear(@RequestParam(defaultValue = "90") Integer beforeDays) {
        auditLogService.clearBeforeDays(beforeDays);
        return R.ok();
    }

}