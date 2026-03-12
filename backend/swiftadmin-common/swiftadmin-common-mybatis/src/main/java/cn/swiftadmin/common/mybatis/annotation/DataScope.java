package cn.swiftadmin.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * <p>用于标注需要进行数据权限过滤的方法</p>
 * <p>示例：@DataScope(deptAlias = "d", userAlias = "u")</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门表的别名
     */
    String deptAlias() default "d";

    /**
     * 用户表的别名
     */
    String userAlias() default "u";

    /**
     * 部门 ID 字段名
     */
    String deptIdField() default "dept_id";

    /**
     * 用户 ID 字段名
     */
    String userIdField() default "user_id";

}