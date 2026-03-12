package cn.swiftadmin.common.security.annotation;

import java.lang.annotation.*;

/**
 * 忽略审计字段注解
 * <p>标注在实体字段上，审计时忽略该字段</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditIgnore {

    /**
     * 忽略原因
     */
    String value() default "";

}