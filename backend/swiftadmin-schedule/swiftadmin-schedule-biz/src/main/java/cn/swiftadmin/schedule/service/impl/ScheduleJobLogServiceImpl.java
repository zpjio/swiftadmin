package cn.swiftadmin.schedule.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.schedule.api.dto.JobLogQueryDTO;
import cn.swiftadmin.schedule.api.entity.ScheduleJobLog;
import cn.swiftadmin.schedule.api.service.ScheduleJobLogService;
import cn.swiftadmin.schedule.api.vo.JobLogVO;
import cn.swiftadmin.schedule.mapper.ScheduleJobLogMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static cn.swiftadmin.schedule.api.entity.table.ScheduleJobLogTableDef.SCHEDULE_JOB_LOG;

/**
 * 定时任务日志服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Slf4j
@Service
public class ScheduleJobLogServiceImpl extends ServiceImpl<ScheduleJobLogMapper, ScheduleJobLog> implements ScheduleJobLogService {

    @Override
    public Page<JobLogVO> pageQuery(JobLogQueryDTO queryDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SCHEDULE_JOB_LOG.JOB_ID.eq(queryDTO.getJobId(), () -> queryDTO.getJobId() != null))
                .and(SCHEDULE_JOB_LOG.JOB_NAME.like(queryDTO.getJobName(), StringUtils::hasText))
                .and(SCHEDULE_JOB_LOG.JOB_GROUP.eq(queryDTO.getJobGroup(), StringUtils::hasText))
                .and(SCHEDULE_JOB_LOG.EXECUTE_STATUS.eq(queryDTO.getExecuteStatus(), () -> queryDTO.getExecuteStatus() != null))
                .and(SCHEDULE_JOB_LOG.EXECUTE_TYPE.eq(queryDTO.getExecuteType(), () -> queryDTO.getExecuteType() != null))
                .and(SCHEDULE_JOB_LOG.EXECUTE_TIME.ge(queryDTO.getExecuteTimeBegin(), () -> queryDTO.getExecuteTimeBegin() != null))
                .and(SCHEDULE_JOB_LOG.EXECUTE_TIME.le(queryDTO.getExecuteTimeEnd(), () -> queryDTO.getExecuteTimeEnd() != null))
                .orderBy(SCHEDULE_JOB_LOG.EXECUTE_TIME.desc());

        return mapper.paginateAs(PageHelper.of(queryDTO.getPage(), queryDTO.getPageSize()), queryWrapper, JobLogVO.class);
    }

    @Override
    public JobLogVO getLogDetail(Long logId) {
        ScheduleJobLog jobLog = getById(logId);
        if (jobLog == null) {
            throw BusinessException.of(400, "日志不存在");
        }
        return convertToVO(jobLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLog(ScheduleJobLog jobLog) {
        save(jobLog);
        return jobLog.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLog(Long logId, Integer executeStatus, String executeResult,
                          String errorMsg, String stackTrace, Long costTime) {
        ScheduleJobLog jobLog = getById(logId);
        if (jobLog == null) {
            log.error("Log not found: {}", logId);
            return;
        }

        jobLog.setExecuteStatus(executeStatus);
        jobLog.setExecuteResult(executeResult);
        jobLog.setErrorMsg(errorMsg);
        jobLog.setStackTrace(stackTrace);
        jobLog.setCostTime(costTime);

        updateById(jobLog);
    }

    /**
     * 转换为 VO
     *
     * @param jobLog 日志实体
     * @return VO
     */
    private JobLogVO convertToVO(ScheduleJobLog jobLog) {
        JobLogVO vo = new JobLogVO();
        vo.setId(jobLog.getId());
        vo.setJobId(jobLog.getJobId());
        vo.setJobName(jobLog.getJobName());
        vo.setJobGroup(jobLog.getJobGroup());
        vo.setExecuteStatus(jobLog.getExecuteStatus());
        vo.setExecuteTime(jobLog.getExecuteTime());
        vo.setCostTime(jobLog.getCostTime());
        vo.setExecuteResult(jobLog.getExecuteResult());
        vo.setErrorMsg(jobLog.getErrorMsg());
        vo.setStackTrace(jobLog.getStackTrace());
        vo.setRetryCount(jobLog.getRetryCount());
        vo.setExecuteType(jobLog.getExecuteType());
        vo.setCreateTime(jobLog.getCreateTime());
        return vo;
    }
}
