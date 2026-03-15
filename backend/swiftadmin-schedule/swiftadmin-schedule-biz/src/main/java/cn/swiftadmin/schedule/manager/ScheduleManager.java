package cn.swiftadmin.schedule.manager;

import cn.swiftadmin.schedule.api.entity.ScheduleJob;
import cn.swiftadmin.schedule.api.entity.ScheduleJobLog;
import cn.swiftadmin.schedule.api.enums.JobExecuteStatus;
import cn.swiftadmin.schedule.api.enums.JobStatus;
import cn.swiftadmin.schedule.api.enums.JobType;
import cn.swiftadmin.schedule.api.service.ScheduleJobLogService;
import cn.swiftadmin.schedule.api.service.ScheduleJobService;
import cn.swiftadmin.schedule.executor.JobExecutor;
import cn.swiftadmin.schedule.executor.impl.BeanJobExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调度管理器
 * <p>负责任务调度、执行、重试等核心逻辑</p>
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Slf4j
@Component
public class ScheduleManager {

    private final ScheduleJobService jobService;
    private final ScheduleJobLogService logService;
    private final Map<JobType, JobExecutor> executorMap;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ScheduleManager(ScheduleJobService jobService,
                           ScheduleJobLogService logService,
                           List<JobExecutor> executors) {
        this.jobService = jobService;
        this.logService = logService;
        this.executorMap = new ConcurrentHashMap<>();
        for (JobExecutor executor : executors) {
            executorMap.put(executor.getType(), executor);
        }
    }

    /**
     * 定时扫描并执行任务
     * <p>每5秒执行一次，检查是否有需要执行的任务</p>
     */
    @Scheduled(fixedRate = 5000)
    public void scheduleJobs() {
        try {
            // 获取所有启用的任务
            List<ScheduleJob> jobs = jobService.getAllEnabledJobs();
            if (jobs == null || jobs.isEmpty()) {
                return;
            }

            Date now = new Date();
            for (ScheduleJob job : jobs) {
                try {
                    // 检查任务是否需要执行
                    if (shouldExecute(job, now)) {
                        // 异步执行任务
                        executeJob(job, JobExecuteStatus.RUNNING);
                    }
                } catch (Exception e) {
                    log.error("Error scheduling job: {}", job.getJobName(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in schedule jobs", e);
        }
    }

    /**
     * 手动触发任务
     *
     * @param jobId 任务ID
     * @return 执行日志ID
     */
    public Long triggerJob(Long jobId) {
        ScheduleJob job = jobService.getById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        if (JobStatus.PAUSED.getCode().equals(job.getStatus())) {
            throw new IllegalArgumentException("Job is paused: " + job.getJobName());
        }

        return executeJob(job, JobExecuteStatus.RUNNING);
    }

    /**
     * 执行任务
     *
     * @param job 任务信息
     * @param executeStatus 执行状态
     * @return 执行日志ID
     */
    private Long executeJob(ScheduleJob job, JobExecuteStatus executeStatus) {
        long startTime = System.currentTimeMillis();
        Long logId = createJobLog(job, executeStatus);

        try {
            // 获取执行器（默认 Bean 类型，后续可扩展分布式等类型）
            JobType jobType = job.toJobType();
            JobExecutor executor = executorMap.get(jobType);
            if (executor == null) {
                throw new IllegalStateException("No executor found for job type: " + jobType + ", job: " + job.getJobName());
            }

            // 执行任务
            Object result = executor.execute(job);

            // 记录成功日志
            long costTime = System.currentTimeMillis() - startTime;
            String resultJson = objectMapper.writeValueAsString(result);
            logService.updateLog(logId, JobExecuteStatus.SUCCESS.getCode(), resultJson, null, null, costTime);

            log.info("Job executed successfully: {}, cost: {}ms, result: {}", job.getJobName(), costTime, resultJson);
            return logId;

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("Job execution failed: {}, cost: {}ms", job.getJobName(), costTime, e);

            // 记录失败日志
            logService.updateLog(logId, JobExecuteStatus.FAILED.getCode(), null,
                    e.getMessage(), getStackTrace(e), costTime);

            // 判断是否需要重试
            if (job.getRetryTimes() != null && job.getRetryTimes() > 0) {
                retryJob(job, logId, e);
            }

            return logId;
        }
    }

    /**
     * 重试任务
     *
     * @param job 任务信息
     * @param originalLogId 原始日志ID
     * @param originalError 原始错误
     */
    private void retryJob(ScheduleJob job, Long originalLogId, Exception originalError) {
        int retryTimes = job.getRetryTimes();
        int retryInterval = job.getRetryInterval() != null ? job.getRetryInterval() : 0;

        log.info("Starting retry for job: {}, retryTimes: {}, retryInterval: {}s",
                job.getJobName(), retryTimes, retryInterval);

        for (int i = 1; i <= retryTimes; i++) {
            try {
                // 等待重试间隔
                if (retryInterval > 0) {
                    Thread.sleep(retryInterval * 1000L);
                }

                long startTime = System.currentTimeMillis();
                Long logId = createJobLog(job, JobExecuteStatus.RUNNING);
                ScheduleJobLog jobLog = logService.getById(logId);
                jobLog.setRetryCount(i);
                logService.updateById(jobLog);

                // 执行任务
                JobType jobType = job.toJobType();
                JobExecutor executor = executorMap.get(jobType);
                Object result = executor.execute(job);

                // 记录成功日志
                long costTime = System.currentTimeMillis() - startTime;
                String resultJson = objectMapper.writeValueAsString(result);
                logService.updateLog(logId, JobExecuteStatus.SUCCESS.getCode(), resultJson, null, null, costTime);

                log.info("Job retry {} succeeded: {}, cost: {}ms", i, job.getJobName(), costTime);
                return;

            } catch (Exception e) {
                log.warn("Job retry {} failed: {}, error: {}", i, job.getJobName(), e.getMessage());
                if (i == retryTimes) {
                    log.error("Job retry exhausted after {} attempts: {}", retryTimes, job.getJobName(), e);
                }
            }
        }
    }

    /**
     * 创建任务执行日志
     *
     * @param job 任务信息
     * @param executeStatus 执行状态
     * @return 日志ID
     */
    private Long createJobLog(ScheduleJob job, JobExecuteStatus executeStatus) {
        ScheduleJobLog jobLog = ScheduleJobLog.builder()
                .jobId(job.getId())
                .jobName(job.getJobName())
                .jobGroup(job.getJobGroup())
                .executeStatus(executeStatus.getCode())
                .executeTime(LocalDateTime.now())
                .retryCount(0)
                .executeType(1) // 自动执行
                .build();

        return logService.createLog(jobLog);
    }

    /**
     * 判断任务是否应该执行
     *
     * @param job 任务信息
     * @param now 当前时间
     * @return true=应该执行，false=不应该执行
     */
    private boolean shouldExecute(ScheduleJob job, Date now) {
        try {
            CronExpression cronExpression = CronExpression.parse(job.getCronExpression());

            LocalDateTime nowLocal = LocalDateTime.now();
            LocalDateTime nextExecuteTime = cronExpression.next(nowLocal.minusSeconds(5));

            if (nextExecuteTime == null) {
                return false;
            }

            long timeDiff = java.time.Duration.between(nowLocal, nextExecuteTime).toMillis();
            return timeDiff >= 0 && timeDiff <= 5000;

        } catch (Exception e) {
            log.error("Error checking if job should execute: {}", job.getJobName(), e);
            return false;
        }
    }

    /**
     * 获取异常堆栈
     *
     * @param e 异常
     * @return 堆栈字符串
     */
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
