package cn.swiftadmin.common.core.annotation;

import java.lang.annotation.*;

/**
 * 需要同意用户协议和隐私政策的注解
 * <p>标记此注解的接口会检查用户是否已同意用户协议和隐私政策</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAgreement {

    /**
     * 是否需要检查用户协议
     *
     * @return true=检查，false=不检查
     */
    boolean checkUserAgreement() default true;

    /**
     * 是否需要检查隐私政策
     *
     * @return true=检查，false=不检查
     */
    boolean checkPrivacyPolicy() default true;
}
