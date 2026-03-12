package cn.swiftadmin.common.core.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 * <p>基于 Redis + Lua 实现分布式限流，支持接口级、用户级、IP 级限流</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口大小
     *
     * @return 时间窗口数值
     */
    int timeWindow() default 60;

    /**
     * 时间单位
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 时间窗口内最大请求数
     *
     * @return 最大请求数
     */
    int maxRequests() default 10;

    /**
     * 限流 key，支持自定义
     * <p>为空时使用 IP + 方法 + 路径作为 key</p>
     *
     * @return 限流 key
     */
    String key() default "";

    /**
     * 限流提示信息
     *
     * @return 提示信息
     */
    String message() default "请求过于频繁，请稍后再试";

}
