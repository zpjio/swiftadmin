package cn.swiftadmin.common.mybatis.aspect;

import cn.swiftadmin.common.mybatis.annotation.DataScope;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 数据权限切面
 * <p>拦截标注了 @DataScope 注解的方法，通过 ThreadLocal 传递配置，在 QueryWrapper 创建时自动应用权限过滤</p>
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
 * @since 2026-03-10
 */
@Slf4j
@Aspect
public class DataScopeAspect {

    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_CUSTOM = 2;
    private static final int SCOPE_DEPT = 3;
    private static final int SCOPE_DEPT_AND_CHILD = 4;
    private static final int SCOPE_SELF = 5;

    /** 忽略数据权限标记 */
    private static final ThreadLocal<Boolean> IGNORE_FLAG = new ThreadLocal<>();

    /** 当前方法的 @DataScope 配置 */
    private static final ThreadLocal<DataScopeConfig> DATA_SCOPE_CONFIG = new ThreadLocal<>();

    private static SecurityContextHolder contextHolder;

    public static void setContextHolder(SecurityContextHolder holder) {
        contextHolder = holder;
    }

    @Before("@annotation(cn.swiftadmin.common.mybatis.annotation.DataScope)")
    public void doBefore(JoinPoint point) {
        if (Boolean.TRUE.equals(IGNORE_FLAG.get())) {
            return;
        }

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        DataScope dataScope = method.getAnnotation(DataScope.class);

        // 存储注解配置到 ThreadLocal
        DATA_SCOPE_CONFIG.set(new DataScopeConfig(
                dataScope.userAlias(),
                dataScope.deptIdField(),
                dataScope.userIdField()
        ));
    }

    @After("@annotation(cn.swiftadmin.common.mybatis.annotation.DataScope)")
    public void doAfter() {
        IGNORE_FLAG.remove();
        DATA_SCOPE_CONFIG.remove();
    }

    /**
     * 跳过数据权限执行
     */
    public static <T> T withoutDataScope(Supplier<T> supplier) {
        IGNORE_FLAG.set(Boolean.TRUE);
        try {
            return supplier.get();
        } finally {
            IGNORE_FLAG.remove();
        }
    }

    /**
     * 跳过数据权限执行
     */
    public static void withoutDataScope(Runnable runnable) {
        IGNORE_FLAG.set(Boolean.TRUE);
        try {
            runnable.run();
        } finally {
            IGNORE_FLAG.remove();
        }
    }

    /**
     * 应用当前方法 @DataScope 注解配置的数据权限
     * <p>由 QueryWrappers.create() 自动调用</p>
     */
    @SuppressWarnings("unchecked")
    public static void applyCurrentDataScope(QueryWrapper qw) {
        if (Boolean.TRUE.equals(IGNORE_FLAG.get())) {
            return;
        }

        DataScopeUser user = getCurrentUser();
        if (user == null || !user.needFilter()) {
            return;
        }

        DataScopeConfig config = DATA_SCOPE_CONFIG.get();
        if (config == null) {
            log.debug("未找到 @DataScope 配置，跳过数据权限过滤");
            return;
        }

        applyDataScopeInternal(qw, user, config.userAlias, config.deptIdField, config.userIdField);
    }

    /**
     * 手动应用数据权限（不依赖注解）
     */
    @SuppressWarnings("unchecked")
    public static void applyDataScope(QueryWrapper qw, String userAlias, String deptIdField, String userIdField) {
        if (Boolean.TRUE.equals(IGNORE_FLAG.get())) {
            return;
        }

        DataScopeUser user = getCurrentUser();
        if (user == null || !user.needFilter()) {
            return;
        }

        applyDataScopeInternal(qw, user, userAlias, deptIdField, userIdField);
    }

    @SuppressWarnings("unchecked")
    private static void applyDataScopeInternal(QueryWrapper qw, DataScopeUser user,
                                                String userAlias, String deptIdField, String userIdField) {
        String deptColumn = userAlias + "." + deptIdField;
        String userColumn = userAlias + "." + userIdField;

        switch (user.scope) {
            case SCOPE_CUSTOM -> {
                if (user.customDeptIds != null && !user.customDeptIds.isEmpty()) {
                    qw.and(deptColumn + " IN (" + placeholders(user.customDeptIds.size()) + ")",
                            user.customDeptIds.toArray());
                }
            }
            case SCOPE_DEPT -> {
                if (user.deptId != null) {
                    qw.and(deptColumn + " = ?", user.deptId);
                }
            }
            case SCOPE_DEPT_AND_CHILD -> applyDeptAndChild(qw, user, deptColumn);
            case SCOPE_SELF -> {
                if (user.userId != null) {
                    qw.and(userColumn + " = ?", user.userId);
                }
            }
        }
        log.debug("数据权限过滤: scope={}, userId={}", user.scope, user.userId);
    }

    @SuppressWarnings("unchecked")
    private static void applyDeptAndChild(QueryWrapper qw, DataScopeUser user, String deptColumn) {
        if (user.deptId == null) return;
        if (user.deptAncestors != null && !user.deptAncestors.isEmpty()) {
            String pattern = user.deptAncestors + "," + user.deptId;
            qw.and(deptColumn + " IN (SELECT id FROM sys_dept WHERE id = ? OR ancestors LIKE ? OR ancestors LIKE ?)",
                    user.deptId, pattern + ",%", pattern);
        } else {
            qw.and(deptColumn + " = ?", user.deptId);
        }
    }

    private static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private static DataScopeUser getCurrentUser() {
        return contextHolder != null ? contextHolder.getCurrentUser() : null;
    }

    /** @DataScope 注解配置 */
    private record DataScopeConfig(String userAlias, String deptIdField, String userIdField) {}

    public interface SecurityContextHolder {
        DataScopeUser getCurrentUser();
    }

    public static class DataScopeUser {
        private Long userId;
        private Long deptId;
        private String deptAncestors;
        private Integer scope;
        private List<Long> customDeptIds;

        public void setUserId(Long userId) { this.userId = userId; }
        public void setDeptId(Long deptId) { this.deptId = deptId; }
        public void setDeptAncestors(String deptAncestors) { this.deptAncestors = deptAncestors; }
        public void setScope(Integer scope) { this.scope = scope; }
        public void setCustomDeptIds(List<Long> customDeptIds) { this.customDeptIds = customDeptIds; }

        public Long getUserId() { return userId; }
        public Long getDeptId() { return deptId; }
        public Integer getScope() { return scope; }

        public boolean needFilter() {
            return scope != null && scope != SCOPE_ALL;
        }
    }
}