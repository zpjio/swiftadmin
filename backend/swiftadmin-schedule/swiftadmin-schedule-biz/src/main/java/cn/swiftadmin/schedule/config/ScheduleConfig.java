package cn.swiftadmin.schedule.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * 定时任务配置
 * <p>配置调度器线程池用于定时任务执行</p>
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {

    /**
     * 定时任务线程池
     * <p>核心线程数 10，支持优雅关闭</p>
     *
     * @return 线程池调度器
     */
    @Bean("scheduleExecutor")
    public Executor scheduleExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("schedule-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }

}
