package cn.swiftadmin.schedule.api.service;

import cn.swiftadmin.schedule.api.entity.ScheduleJob;
import cn.swiftadmin.schedule.api.vo.JobVO;
import cn.swiftadmin.schedule.api.dto.JobQueryDTO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 定时任务服务接口
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
public interface ScheduleJobService extends IService<ScheduleJob> {

    /**
     * 分页查询任务列表
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<JobVO> pageQuery(JobQueryDTO queryDTO);

    /**
     * 根据ID获取任务详情
     *
     * @param jobId 任务ID
     * @return 任务详情
     */
    JobVO getJobDetail(Long jobId);

    /**
     * 创建任务
     *
     * @param job 任务信息
     * @return 任务ID
     */
    Long createJob(ScheduleJob job);

    /**
     * 更新任务
     *
     * @param job 任务信息
     */
    void updateJob(ScheduleJob job);

    /**
     * 删除任务
     *
     * @param jobIds 任务ID列表
     */
    void deleteJob(Long[] jobIds);

    /**
     * 启用任务
     *
     * @param jobId 任务ID
     */
    void enableJob(Long jobId);

    /**
     * 暂停任务
     *
     * @param jobId 任务ID
     */
    void pauseJob(Long jobId);

    /**
     * 手动触发任务
     *
     * @param jobId 任务ID
     * @return 执行日志ID
     */
    Long triggerJob(Long jobId);

    /**
     * 检查任务名称是否存在
     *
     * @param jobName 任务名称
     * @param jobGroup 任务分组
     * @param excludeId 排除的任务ID（用于更新时检查）
     * @return true=存在，false=不存在
     */
    boolean existsByJobName(String jobName, String jobGroup, Long excludeId);

    /**
     * 获取所有启用的任务
     *
     * @return 任务列表
     */
    List<ScheduleJob> getAllEnabledJobs();
}
