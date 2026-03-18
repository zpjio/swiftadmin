package cn.swiftadmin.schedule.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.common.security.annotation.OperationLog.BusinessType;
import cn.swiftadmin.schedule.api.dto.JobDTO;
import cn.swiftadmin.schedule.api.dto.JobQueryDTO;
import cn.swiftadmin.schedule.api.entity.ScheduleJob;
import cn.swiftadmin.schedule.api.service.ScheduleJobService;
import cn.swiftadmin.schedule.api.vo.JobVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务管理控制器
 * <p>提供定时任务的创建、编辑、删除、启用、暂停、手动触发等功能</p>
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Tag(name = "定时任务管理", description = "定时任务配置管理接口")
@RestController
@RequestMapping("/api/schedule/job")
@RequiredArgsConstructor
public class ScheduleJobController {

    private final ScheduleJobService jobService;

    /**
     * 分页查询任务列表
     *
     * @param queryDTO 查询条件
     * @return 任务分页列表
     */
    @Operation(summary = "分页查询任务", description = "支持按任务名称、分组、状态等条件筛选")
    @GetMapping("/page")
    @RequiresPermission("schedule:job:list")
    public R<Page<JobVO>> page(JobQueryDTO queryDTO) {
        return R.ok(jobService.pageQuery(queryDTO));
    }

    /**
     * 获取任务详情
     *
     * @param id 任务ID
     * @return 任务详情
     */
    @Operation(summary = "获取任务详情", description = "根据任务 ID 获取完整信息")
    @GetMapping("/{id}")
    @RequiresPermission("schedule:job:query")
    public R<JobVO> getDetail(@PathVariable Long id) {
        return R.ok(jobService.getJobDetail(id));
    }

    /**
     * 创建任务
     *
     * @param dto 任务信息
     * @return 任务ID
     */
    @Operation(summary = "创建任务", description = "创建新的定时任务")
    @PostMapping
    @RequiresPermission("schedule:job:add")
    @OperationLog(title = "定时任务", businessType = BusinessType.INSERT,
            entityClass = ScheduleJob.class)
    public R<Long> create(@Valid @RequestBody JobDTO dto) {
        ScheduleJob job = new ScheduleJob();
        BeanUtils.copyProperties(dto, job);
        Long jobId = jobService.createJob(job);
        return R.ok(jobId);
    }

    /**
     * 更新任务
     *
     * @param dto 任务信息
     */
    @Operation(summary = "更新任务", description = "更新定时任务配置")
    @PutMapping
    @RequiresPermission("schedule:job:edit")
    @OperationLog(title = "定时任务", businessType = BusinessType.UPDATE,
            entityClass = ScheduleJob.class)
    public R<Void> update(@Valid @RequestBody JobDTO dto) {
        ScheduleJob job = new ScheduleJob();
        BeanUtils.copyProperties(dto, job);
        jobService.updateJob(job);
        return R.ok();
    }

    /**
     * 删除任务
     *
     * @param ids 任务ID列表（逗号分隔）
     */
    @Operation(summary = "删除任务", description = "删除定时任务")
    @DeleteMapping("/{ids}")
    @RequiresPermission("schedule:job:delete")
    @OperationLog(title = "定时任务", businessType = BusinessType.DELETE,
            entityClass = ScheduleJob.class)
    public R<Void> delete(@PathVariable Long[] ids) {
        jobService.deleteJob(ids);
        return R.ok();
    }

    /**
     * 启用任务
     *
     * @param id 任务ID
     */
    @Operation(summary = "启用任务", description = "启用定时任务，开始按计划执行")
    @PutMapping("/{id}/enable")
    @RequiresPermission("schedule:job:enable")
    @OperationLog(title = "定时任务", businessType = BusinessType.UPDATE,
            entityClass = ScheduleJob.class)
    public R<Void> enable(@PathVariable Long id) {
        jobService.enableJob(id);
        return R.ok();
    }

    /**
     * 暂停任务
     *
     * @param id 任务ID
     */
    @Operation(summary = "暂停任务", description = "暂停定时任务，停止执行")
    @PutMapping("/{id}/pause")
    @RequiresPermission("schedule:job:disable")
    @OperationLog(title = "定时任务", businessType = BusinessType.UPDATE,
            entityClass = ScheduleJob.class)
    public R<Void> pause(@PathVariable Long id) {
        jobService.pauseJob(id);
        return R.ok();
    }

    /**
     * 手动触发任务
     *
     * @param id 任务ID
     * @return 执行日志ID
     */
    @Operation(summary = "手动触发任务", description = "立即执行一次任务")
    @PostMapping("/{id}/trigger")
    @RequiresPermission("schedule:job:trigger")
    @OperationLog(title = "定时任务", businessType = BusinessType.OTHER)
    public R<Long> trigger(@PathVariable Long id) {
        Long logId = jobService.triggerJob(id);
        return R.ok(logId);
    }
}
