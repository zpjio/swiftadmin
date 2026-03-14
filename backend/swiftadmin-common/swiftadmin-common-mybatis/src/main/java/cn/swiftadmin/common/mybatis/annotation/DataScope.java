package cn.swiftadmin.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * <p>标注在 Service 方法上，自动应用数据权限过滤</p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 基本用法：只需指定用户表别名
 * &#64;DataScope(userAlias = "u")
 * public Page&lt;UserVO&gt; page(UserQueryDTO query) {
 *     QueryWrapper queryWrapper = QueryWrapper.create()
 *         .from(SYS_USER.as("u"))
 *         .leftJoin(SYS_DEPT.as("d")).on(SYS_USER.DEPT_ID.eq(SYS_DEPT.ID))
 *         ...;
 *     return mapper.paginateAs(page, queryWrapper, UserVO.class);
 * }
 * </pre>
 *
 * <h3>数据权限范围说明：</h3>
 * <ul>
 *   <li>全部数据(1)：不添加过滤条件</li>
 *   <li>自定义(2)：过滤用户 dept_id 在指定部门列表中</li>
 *   <li>本部门(3)：过滤用户 dept_id = 当前用户部门</li>
 *   <li>本部门及以下(4)：通过部门祖先路径过滤</li>
 *   <li>仅本人(5)：过滤 user_id = 当前用户ID</li>
 * </ul>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 用户表别名
     * <p>必须与 SQL 中用户表的别名一致</p>
     * <p>示例：FROM sys_user AS u，则 userAlias = "u"</p>
     */
    String userAlias();

    /**
     * 部门 ID 字段名
     * <p>默认为 dept_id，通常无需修改</p>
     */
    String deptIdField() default "dept_id";

    /**
     * 用户 ID 字段名
     * <p>默认为 id，用于"仅本人"权限范围</p>
     */
    String userIdField() default "id";

}