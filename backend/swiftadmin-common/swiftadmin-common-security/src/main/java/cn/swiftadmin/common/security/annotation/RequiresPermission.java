package cn.swiftadmin.common.security.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * <p>用于标注需要进行权限校验的方法</p>
 * <p>示例：@RequiresPermission("system:user:list")</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 需要校验的权限标识
     * <p>支持多个权限，满足其中一个即可</p>
     */
    String[] value();

    /**
     * 多个权限的逻辑关系
     * <p>AND: 需要满足所有权限</p>
     * <p>OR: 满足其中一个权限即可（默认）</p>
     */
    Logical logical() default Logical.OR;

    /**
     * 逻辑关系枚举
     */
    enum Logical {
        AND,
        OR
    }

}