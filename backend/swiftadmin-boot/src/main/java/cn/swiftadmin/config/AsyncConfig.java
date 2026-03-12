package cn.swiftadmin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 * <p>配置线程池用于异步执行登录日志记录等任务</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 异步任务线程池
     * <p>核心线程数 5，最大线程数 10，队列容量 100</p>
     *
     * @return 线程池执行器
     */
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        // 队列满时由调用线程执行
        executor.setRejectedExecutionHandler((r, e) -> {
            r.run();
        });
        executor.initialize();
        return executor;
    }

}
