package cn.swiftadmin.common.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 允许被定时任务调度器反射调用。
 * <p>标在类上时，该类声明的 public 方法都可被调度；标在方法上时只放行该方法。
 * 未标注的 Bean 不能作为 Bean 任务目标。</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledJobTarget {
}
