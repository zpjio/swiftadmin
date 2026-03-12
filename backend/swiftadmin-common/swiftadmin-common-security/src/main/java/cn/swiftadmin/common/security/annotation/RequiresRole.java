package cn.swiftadmin.common.security.annotation;

import java.lang.annotation.*;

/**
 * 角色校验注解
 * <p>用于标注需要进行角色校验的方法</p>
 * <p>示例：@RequiresRole("admin")</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {

    /**
     * 需要校验的角色编码
     * <p>支持多个角色，满足其中一个即可</p>
     */
    String[] value();

    /**
     * 多个角色的逻辑关系
     * <p>AND: 需要满足所有角色</p>
     * <p>OR: 满足其中一个角色即可（默认）</p>
     */
    RequiresPermission.Logical logical() default RequiresPermission.Logical.OR;

}