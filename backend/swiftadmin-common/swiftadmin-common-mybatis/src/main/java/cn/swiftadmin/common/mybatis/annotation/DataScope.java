package cn.swiftadmin.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * <p>标注在 Service 方法上，配合 QueryWrappers.create() 自动应用数据权限过滤</p>
 *
 * <h3>使用方式：</h3>
 * <pre>
 * // 1. 在方法上添加注解
 * // 2. 使用 QueryWrappers.create() 创建 QueryWrapper
 *
 * &#64;DataScope(userAlias = "u", userIdField = "id")
 * public Page&lt;UserVO&gt; page(UserQueryDTO query) {
 *     QueryWrapper qw = QueryWrappers.create()
 *         .select(SYS_USER.ALL_COLUMNS)
 *         .from(SYS_USER.as("u"))
 *         ...;
 *     return mapper.paginateAs(page, qw, UserVO.class);
 * }
 * </pre>
 *
 * <h3>字段说明：</h3>
 * <ul>
 *   <li>userAlias: 用户表别名，必须与 SQL 中的别名一致</li>
 *   <li>deptIdField: 部门ID字段名，默认 dept_id</li>
 *   <li>userIdField: 用户ID字段名，默认 user_id（sys_user 表用 id）</li>
 * </ul>
 *
 * <h3>数据权限范围：</h3>
 * <ul>
 *   <li>全部数据(1)：不添加过滤条件</li>
 *   <li>自定义(2)：dept_id 在指定部门列表中</li>
 *   <li>本部门(3)：dept_id = 当前用户部门</li>
 *   <li>本部门及以下(4)：dept_id 在当前部门及子部门</li>
 *   <li>仅本人(5)：userIdField = 当前用户ID</li>
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
     * <p>默认为 user_id（业务表常见字段名）</p>
     * <p>查询 sys_user 表时需指定为 id</p>
     * <p>示例：@DataScope(userAlias = "u", userIdField = "id")</p>
     */
    String userIdField() default "user_id";

}