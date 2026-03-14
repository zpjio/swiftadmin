package cn.swiftadmin.common.mybatis.util;

import cn.swiftadmin.common.mybatis.aspect.DataScopeAspect;
import com.mybatisflex.core.query.QueryWrapper;

/**
 * 数据权限 QueryWrapper 工具类
 * <p>配合 @DataScope 注解使用，自动应用数据权限过滤</p>
 *
 * <h3>使用方式：</h3>
 * <pre>
 * &#64;DataScope(userAlias = "u", userIdField = "id")
 * public Page&lt;UserVO&gt; page(UserQueryDTO query) {
 *     QueryWrapper qw = QueryWrappers.create()
 *         .select(...)
 *         .from(SYS_USER.as("u"))
 *         ...;
 *     return mapper.paginateAs(page, qw, UserVO.class);
 * }
 * </pre>
 *
 * @author zhangpengju
 * @since 2026-03-14
 */
public class QueryWrappers {

    /**
     * 创建 QueryWrapper 并自动应用数据权限
     * <p>需要在方法上标注 @DataScope 注解</p>
     */
    public static QueryWrapper create() {
        QueryWrapper qw = QueryWrapper.create();
        DataScopeAspect.applyCurrentDataScope(qw);
        return qw;
    }

    /**
     * 创建 QueryWrapper 并自动应用数据权限（带别名）
     * <p>适用于简单场景，不需要 @DataScope 注解</p>
     *
     * @param userAlias 用户表别名
     */
    public static QueryWrapper create(String userAlias) {
        QueryWrapper qw = QueryWrapper.create();
        DataScopeAspect.applyDataScope(qw, userAlias, "dept_id", "user_id");
        return qw;
    }

}