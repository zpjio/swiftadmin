package cn.swiftadmin.schedule.executor;

import cn.swiftadmin.schedule.api.entity.ScheduleJob;
import cn.swiftadmin.schedule.api.enums.JobType;

/**
 * 任务执行器接口
 * <p>用于扩展不同类型的任务执行方式，如 Bean、HTTP、Shell 等</p>
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
public interface JobExecutor {

    /**
     * 当前执行器支持的任务类型
     *
     * @return 任务类型
     */
    JobType getType();

    /**
     * 执行任务
     *
     * @param job 任务信息
     * @return 执行结果
     * @throws Exception 执行异常
     */
    Object execute(ScheduleJob job) throws Exception;

    /**
     * 验证任务配置是否有效
     *
     * @param job 任务信息
     * @return true=有效，false=无效
     */
    boolean validate(ScheduleJob job);
}
