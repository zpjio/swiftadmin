package cn.swiftadmin.schedule.manager;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.schedule.api.entity.ScheduleJob;
import cn.swiftadmin.schedule.api.entity.ScheduleJobLog;
import cn.swiftadmin.schedule.api.enums.JobExecuteStatus;
import cn.swiftadmin.schedule.api.enums.JobStatus;
import cn.swiftadmin.schedule.api.enums.JobType;
import cn.swiftadmin.schedule.api.service.ScheduleJobLogService;
import cn.swiftadmin.schedule.api.service.ScheduleJobService;
import cn.swiftadmin.schedule.executor.JobExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
@RequiredArgsConstructor
public class ScheduleManager {

    private final ScheduleJobService jobService;
    private final ScheduleJobLogService logService;
    private final List<JobExecutor> executors;

    /** 执行器映射 */
    private Map<JobType, JobExecutor> executorMap;

    /** 正在执行的任务ID集合（防止重复执行） */
    private final Set<Long> runningJobs = ConcurrentHashMap.newKeySet();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化执行器映射
     */
    @PostConstruct
    public void init() {
        executorMap = new ConcurrentHashMap<>();
        for (JobExecutor executor : executors) {
            executorMap.put(executor.getType(), executor);
        }
        log.info("ScheduleManager initialized executors={}", executorMap.keySet());
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

            for (ScheduleJob job : jobs) {
                try {
                    // 检查任务是否需要执行
                    if (shouldExecute(job)) {
                        // 检查任务是否正在执行
                        if (runningJobs.contains(job.getId())) {
                            log.debug("Job is already running, skip: {}", job.getJobName());
                            continue;
                        }
                        // 异步执行任务
                        executeJobAsync(job);
                    }
                } catch (Exception e) {
                    log.error("SCHEDULE_ERROR job={} error={}", job.getJobName(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("SCHEDULE_ERROR error={}", e.getMessage(), e);
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
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }

        if (JobStatus.PAUSED.getCode().equals(job.getStatus())) {
            throw new BusinessException(ErrorCode.JOB_PAUSED);
        }

        return executeJob(job);
    }

    /**
     * 异步执行任务
     *
     * @param job 任务信息
     */
    private void executeJobAsync(ScheduleJob job) {
        CompletableFuture.runAsync(() -> executeJob(job));
    }

    /**
     * 执行任务
     *
     * @param job 任务信息
     * @return 执行日志ID
     */
    private Long executeJob(ScheduleJob job) {
        // 标记任务开始执行
        runningJobs.add(job.getId());

        long startTime = System.currentTimeMillis();
        Long logId = createJobLog(job, JobExecuteStatus.RUNNING);

        try {
            // 获取执行器（默认 Bean 类型，后续可扩展分布式等类型）
            JobType jobType = job.toJobType();
            JobExecutor executor = executorMap.get(jobType);
            if (executor == null) {
                throw new BusinessException(ErrorCode.JOB_EXECUTOR_NOT_FOUND.getCode(),
                        "未找到任务执行器: " + jobType);
            }

            // 执行任务
            Object result = executor.execute(job);

            // 记录成功日志
            long costTime = System.currentTimeMillis() - startTime;
            String resultJson = objectMapper.writeValueAsString(result);
            logService.updateLog(logId, JobExecuteStatus.SUCCESS.getCode(), resultJson, null, null, costTime);

            log.info("JOB_EXECUTED job={} status=SUCCESS cost={}ms", job.getJobName(), costTime);
            return logId;

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("JOB_EXECUTED job={} status=FAILED cost={}ms error={}", job.getJobName(), costTime, e.getMessage());

            // 记录失败日志
            logService.updateLog(logId, JobExecuteStatus.FAILED.getCode(), null,
                    e.getMessage(), getStackTrace(e), costTime);

            // 判断是否需要重试
            if (job.getRetryTimes() != null && job.getRetryTimes() > 0) {
                retryJobAsync(job);
            }

            return logId;
        } finally {
            // 标记任务执行结束
            runningJobs.remove(job.getId());
        }
    }

    /**
     * 异步重试任务
     *
     * @param job 任务信息
     */
    private void retryJobAsync(ScheduleJob job) {
        CompletableFuture.runAsync(() -> retryJob(job));
    }

    /**
     * 重试任务
     *
     * @param job 任务信息
     */
    private void retryJob(ScheduleJob job) {
        int retryTimes = job.getRetryTimes();
        int retryInterval = job.getRetryInterval() != null ? job.getRetryInterval() : 0;

        log.info("JOB_RETRY_START job={} retryTimes={} retryInterval={}s", job.getJobName(), retryTimes, retryInterval);

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

                log.info("JOB_RETRY_SUCCESS job={} attempt={} cost={}ms", job.getJobName(), i, costTime);
                return;

            } catch (Exception e) {
                log.warn("JOB_RETRY_FAILED job={} attempt={} error={}", job.getJobName(), i, e.getMessage());
                if (i == retryTimes) {
                    log.error("JOB_RETRY_EXHAUSTED job={} attempts={}", job.getJobName(), retryTimes, e);
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
     * @return true=应该执行，false=不应该执行
     */
    private boolean shouldExecute(ScheduleJob job) {
        try {
            CronExpression cronExpression = CronExpression.parse(job.getCronExpression());

            LocalDateTime nowLocal = LocalDateTime.now();
            LocalDateTime nextExecuteTime = cronExpression.next(nowLocal.minusSeconds(5));

            if (nextExecuteTime == null) {
                return false;
            }

            long timeDiff = Duration.between(nowLocal, nextExecuteTime).toMillis();
            return timeDiff >= 0 && timeDiff <= 5000;

        } catch (Exception e) {
            log.error("JOB_CRON_ERROR job={} error={}", job.getJobName(), e.getMessage());
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
        return String.join("\n",
                java.util.Arrays.stream(e.getStackTrace())
                        .map(StackTraceElement::toString)
                        .toArray(String[]::new));
    }
}
