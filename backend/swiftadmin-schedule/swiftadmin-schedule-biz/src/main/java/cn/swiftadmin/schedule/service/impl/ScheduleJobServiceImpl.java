package cn.swiftadmin.schedule.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.schedule.api.dto.JobDTO;
import cn.swiftadmin.schedule.api.dto.JobQueryDTO;
import cn.swiftadmin.schedule.api.entity.ScheduleJob;
import cn.swiftadmin.schedule.api.enums.JobStatus;
import cn.swiftadmin.schedule.api.enums.JobType;
import cn.swiftadmin.schedule.api.service.ScheduleJobService;
import cn.swiftadmin.schedule.api.vo.JobVO;
import cn.swiftadmin.schedule.executor.impl.BeanJobExecutor;
import cn.swiftadmin.schedule.manager.ScheduleManager;
import cn.swiftadmin.schedule.mapper.ScheduleJobMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static cn.swiftadmin.schedule.api.entity.table.ScheduleJobTableDef.SCHEDULE_JOB;

/**
 * 定时任务服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Slf4j
@Service
public class ScheduleJobServiceImpl extends ServiceImpl<ScheduleJobMapper, ScheduleJob> implements ScheduleJobService {

    private final BeanJobExecutor beanJobExecutor;
    private final ScheduleManager scheduleManager;

    public ScheduleJobServiceImpl(BeanJobExecutor beanJobExecutor,
                                   @Lazy ScheduleManager scheduleManager) {
        this.beanJobExecutor = beanJobExecutor;
        this.scheduleManager = scheduleManager;
    }

    @Override
    public Page<JobVO> pageQuery(JobQueryDTO queryDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SCHEDULE_JOB.JOB_NAME.like(queryDTO.getJobName(), StringUtils::hasText))
                .and(SCHEDULE_JOB.JOB_GROUP.eq(queryDTO.getJobGroup(), StringUtils::hasText))
                .and(SCHEDULE_JOB.BEAN_NAME.like(queryDTO.getBeanName(), StringUtils::hasText))
                .and(SCHEDULE_JOB.STATUS.eq(queryDTO.getStatus(), () -> queryDTO.getStatus() != null))
                .and(SCHEDULE_JOB.CREATE_TIME.ge(queryDTO.getCreateTimeBegin(), () -> queryDTO.getCreateTimeBegin() != null))
                .and(SCHEDULE_JOB.CREATE_TIME.le(queryDTO.getCreateTimeEnd(), () -> queryDTO.getCreateTimeEnd() != null))
                .orderBy(SCHEDULE_JOB.CREATE_TIME.desc());

        Page<JobVO> voPage = mapper.paginateAs(PageHelper.of(queryDTO.getPage(), queryDTO.getPageSize()), queryWrapper, JobVO.class);

        voPage.getRecords().forEach(vo -> {
            if (JobStatus.NORMAL.getCode().equals(vo.getStatus())) {
                try {
                    CronExpression cronExpression = CronExpression.parse(vo.getCronExpression());
                    LocalDateTime nextExecuteTime = cronExpression.next(LocalDateTime.now());
                    vo.setNextExecuteTime(nextExecuteTime);
                } catch (Exception e) {
                    log.warn("Failed to calculate next execute time for job: {}", vo.getJobName(), e);
                }
            }
        });

        return voPage;
    }

    @Override
    public JobVO getJobDetail(Long jobId) {
        ScheduleJob job = getById(jobId);
        if (job == null) {
            throw BusinessException.of(400, "任务不存在");
        }
        return convertToVO(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createJob(ScheduleJob job) {
        // 检查任务名称是否已存在
        if (existsByJobName(job.getJobName(), job.getJobGroup(), null)) {
            throw BusinessException.of(400, "任务名称已存在");
        }

        // 验证 Cron 表达式
        if (!isValidCronExpression(job.getCronExpression())) {
            throw BusinessException.of(400, "Cron 表达式无效");
        }

        // 验证任务配置
        if (!beanJobExecutor.validate(job)) {
            throw BusinessException.of(400, "任务配置无效");
        }

        // 设置默认值
        if (job.getStatus() == null) {
            job.setStatus(JobStatus.NORMAL.getCode());
        }
        if (job.getTimeout() == null) {
            job.setTimeout(300);
        }
        if (job.getRetryTimes() == null) {
            job.setRetryTimes(0);
        }
        if (job.getRetryInterval() == null) {
            job.setRetryInterval(0);
        }
        if (job.getJobGroup() == null) {
            job.setJobGroup("DEFAULT");
        }
        if (job.getJobType() == null) {
            job.setJobType(JobType.BEAN.getCode());
        }

        save(job);
        return job.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(ScheduleJob job) {
        ScheduleJob existingJob = getById(job.getId());
        if (existingJob == null) {
            throw BusinessException.of(400, "任务不存在");
        }

        // 检查任务名称是否已存在
        if (existsByJobName(job.getJobName(), job.getJobGroup(), job.getId())) {
            throw BusinessException.of(400, "任务名称已存在");
        }

        // 验证 Cron 表达式
        if (!isValidCronExpression(job.getCronExpression())) {
            throw BusinessException.of(400, "Cron 表达式无效");
        }

        // 验证任务配置
        if (!beanJobExecutor.validate(job)) {
            throw BusinessException.of(400, "任务配置无效");
        }

        // 更新任务
        job.setCreateTime(existingJob.getCreateTime());
        job.setCreateBy(existingJob.getCreateBy());
        updateById(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(Long[] jobIds) {
        for (Long jobId : jobIds) {
            removeById(jobId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableJob(Long jobId) {
        ScheduleJob job = getById(jobId);
        if (job == null) {
            throw BusinessException.of(400, "任务不存在");
        }

        job.setStatus(JobStatus.NORMAL.getCode());
        updateById(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pauseJob(Long jobId) {
        ScheduleJob job = getById(jobId);
        if (job == null) {
            throw BusinessException.of(400, "任务不存在");
        }

        job.setStatus(JobStatus.PAUSED.getCode());
        updateById(job);
    }

    @Override
    public Long triggerJob(Long jobId) {
        return scheduleManager.triggerJob(jobId);
    }

    @Override
    public boolean existsByJobName(String jobName, String jobGroup, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SCHEDULE_JOB.JOB_NAME.eq(jobName))
                .and(SCHEDULE_JOB.JOB_GROUP.eq(jobGroup));

        if (excludeId != null) {
            queryWrapper.and(SCHEDULE_JOB.ID.ne(excludeId));
        }

        return count(queryWrapper) > 0;
    }

    @Override
    public List<ScheduleJob> getAllEnabledJobs() {
        return list(QueryWrapper.create()
                .where(SCHEDULE_JOB.STATUS.eq(JobStatus.NORMAL.getCode()))
                .orderBy(SCHEDULE_JOB.CREATE_TIME.asc()));
    }

    /**
     * 转换为 VO
     *
     * @param job 任务实体
     * @return VO
     */
    private JobVO convertToVO(ScheduleJob job) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setJobName(job.getJobName());
        vo.setJobGroup(job.getJobGroup());
        vo.setBeanName(job.getBeanName());
        vo.setMethodName(job.getMethodName());
        vo.setMethodParams(job.getMethodParams());
        vo.setCronExpression(job.getCronExpression());
        vo.setTimeout(job.getTimeout());
        vo.setRetryTimes(job.getRetryTimes());
        vo.setRetryInterval(job.getRetryInterval());
        vo.setStatus(job.getStatus());
        vo.setRemark(job.getRemark());
        vo.setCreateTime(job.getCreateTime());
        vo.setUpdateTime(job.getUpdateTime());
        return vo;
    }

    /**
     * 验证 Cron 表达式
     *
     * @param cronExpression Cron 表达式
     * @return true=有效，false=无效
     */
    private boolean isValidCronExpression(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
