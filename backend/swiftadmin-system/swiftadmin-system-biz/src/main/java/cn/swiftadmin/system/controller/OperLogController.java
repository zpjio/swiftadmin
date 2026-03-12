package cn.swiftadmin.system.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.dto.OperLogQueryDTO;
import cn.swiftadmin.system.api.entity.SysOperLog;
import cn.swiftadmin.system.api.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志控制器
 * <p>提供系统操作日志的查询和清理功能</p>
 * <p>记录用户的所有操作行为，用于审计和追溯</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "操作日志管理", description = "系统操作日志查询和清理接口")
@RestController
@RequestMapping("/api/system/operlog")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;

    /**
     * 分页查询操作日志
     */
    @Operation(summary = "分页查询操作日志", description = "支持按操作标题、操作人员、业务类型、状态、时间范围等条件筛选")
    @GetMapping("/page")
    @RequiresPermission("system:operlog:list")
    public R<Page<SysOperLog>> page(OperLogQueryDTO query) {
        return R.ok(operLogService.page(query));
    }

    /**
     * 获取操作日志详情
     */
    @Operation(summary = "获取操作日志详情", description = "查看具体的操作日志信息，包括请求参数、返回结果等")
    @GetMapping("/{id}")
    @RequiresPermission("system:operlog:query")
    public R<SysOperLog> getDetail(@PathVariable Long id) {
        return R.ok(operLogService.getById(id));
    }

    /**
     * 批量删除操作日志
     */
    @Operation(summary = "批量删除操作日志", description = "支持同时删除多条日志记录")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:operlog:delete")
    @OperationLog(title = "操作日志", businessType = OperationLog.BusinessType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        operLogService.deleteByIds(ids);
        return R.ok();
    }

    /**
     * 清空所有操作日志
     */
    @Operation(summary = "清空所有操作日志", description = "删除所有操作日志记录，需谨慎使用")
    @DeleteMapping("/clear")
    @RequiresPermission("system:operlog:delete")
    @OperationLog(title = "操作日志", businessType = OperationLog.BusinessType.CLEAN)
    public R<Void> clear() {
        operLogService.clearAll();
        return R.ok();
    }

}