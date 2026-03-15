package cn.swiftadmin.schedule.api.service;

import cn.swiftadmin.schedule.api.entity.ScheduleJobLog;
import cn.swiftadmin.schedule.api.vo.JobLogVO;
import cn.swiftadmin.schedule.api.dto.JobLogQueryDTO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * 定时任务日志服务接口
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
public interface ScheduleJobLogService extends IService<ScheduleJobLog> {

    /**
     * 分页查询日志列表
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<JobLogVO> pageQuery(JobLogQueryDTO queryDTO);

    /**
     * 根据ID获取日志详情
     *
     * @param logId 日志ID
     * @return 日志详情
     */
    JobLogVO getLogDetail(Long logId);

    /**
     * 创建执行日志
     *
     * @param log 日志信息
     * @return 日志ID
     */
    Long createLog(ScheduleJobLog log);

    /**
     * 更新执行日志
     *
     * @param logId 日志ID
     * @param executeStatus 执行状态
     * @param executeResult 执行结果
     * @param errorMsg 错误信息
     * @param stackTrace 异常堆栈
     * @param costTime 耗时（毫秒）
     */
    void updateLog(Long logId, Integer executeStatus, String executeResult,
                   String errorMsg, String stackTrace, Long costTime);
}
