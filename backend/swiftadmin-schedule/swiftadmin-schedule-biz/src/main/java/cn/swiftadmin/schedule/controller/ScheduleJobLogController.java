package cn.swiftadmin.schedule.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.schedule.api.dto.JobLogQueryDTO;
import cn.swiftadmin.schedule.api.service.ScheduleJobLogService;
import cn.swiftadmin.schedule.api.vo.JobLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务日志控制器
 * <p>提供任务执行日志的查询功能</p>
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Tag(name = "定时任务日志", description = "定时任务执行日志接口")
@RestController
@RequestMapping("/api/schedule/log")
@RequiredArgsConstructor
public class ScheduleJobLogController {

    private final ScheduleJobLogService logService;

    /**
     * 分页查询日志列表
     *
     * @param queryDTO 查询条件
     * @return 日志分页列表
     */
    @Operation(summary = "分页查询日志", description = "支持按任务、状态、时间等条件筛选")
    @GetMapping("/page")
    @RequiresPermission("schedule:log:list")
    public R<Page<JobLogVO>> page(JobLogQueryDTO queryDTO) {
        return R.ok(logService.pageQuery(queryDTO));
    }

    /**
     * 获取日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @Operation(summary = "获取日志详情", description = "根据日志 ID 获取完整信息")
    @GetMapping("/{id}")
    @RequiresPermission("schedule:log:query")
    public R<JobLogVO> getDetail(@PathVariable Long id) {
        return R.ok(logService.getLogDetail(id));
    }
}
