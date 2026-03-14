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
import java.util.List;

/**
 * 数据权限切面
 * <p>拦截标注了 @DataScope 注解的方法，自动应用数据权限过滤</p>
 *
 * <h3>使用方式：</h3>
 * <pre>
 * &#64;DataScope(userAlias = "u")
 * public Page&lt;UserVO&gt; page(UserQueryDTO query) {
 *     QueryWrapper queryWrapper = QueryWrapper.create()
 *         .from(SYS_USER.as("u"))
 *         ...
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

    private static final ThreadLocal<Boolean> IGNORE_FLAG = new ThreadLocal<>();
    private static SecurityContextHolder contextHolder;

    public static void setContextHolder(SecurityContextHolder holder) {
        contextHolder = holder;
    }

    @Before("@annotation(cn.swiftadmin.common.mybatis.annotation.DataScope)")
    public void doBefore(JoinPoint point) {
        if (Boolean.TRUE.equals(IGNORE_FLAG.get())) {
            return;
        }

        DataScopeUser user = getCurrentUser();
        if (user == null || !user.needFilter()) {
            return;
        }

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        DataScope dataScope = method.getAnnotation(DataScope.class);

        for (Object arg : point.getArgs()) {
            if (arg instanceof QueryWrapper) {
                applyDataScope((QueryWrapper) arg, user, dataScope);
                return;
            }
        }

        log.warn("未找到 QueryWrapper 参数，数据权限未生效: {}", method.getName());
    }

    @After("@annotation(cn.swiftadmin.common.mybatis.annotation.DataScope)")
    public void doAfter() {
        IGNORE_FLAG.remove();
    }

    public static <T> T withoutDataScope(java.util.function.Supplier<T> supplier) {
        IGNORE_FLAG.set(Boolean.TRUE);
        try {
            return supplier.get();
        } finally {
            IGNORE_FLAG.remove();
        }
    }

    public static void withoutDataScope(Runnable runnable) {
        IGNORE_FLAG.set(Boolean.TRUE);
        try {
            runnable.run();
        } finally {
            IGNORE_FLAG.remove();
        }
    }

    private DataScopeUser getCurrentUser() {
        return contextHolder != null ? contextHolder.getCurrentUser() : null;
    }

    @SuppressWarnings("unchecked")
    private void applyDataScope(QueryWrapper qw, DataScopeUser user, DataScope ds) {
        String deptColumn = ds.userAlias() + "." + ds.deptIdField();

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
                    qw.and(ds.userAlias() + "." + ds.userIdField() + " = ?", user.userId);
                }
            }
        }
        log.debug("数据权限过滤: scope={}", user.scope);
    }

    @SuppressWarnings("unchecked")
    private void applyDeptAndChild(QueryWrapper qw, DataScopeUser user, String deptColumn) {
        if (user.deptId == null) return;
        if (user.deptAncestors != null && !user.deptAncestors.isEmpty()) {
            String pattern = user.deptAncestors + "," + user.deptId;
            qw.and(deptColumn + " IN (SELECT id FROM sys_dept WHERE id = ? OR ancestors LIKE ? OR ancestors LIKE ?)",
                    user.deptId, pattern + ",%", pattern);
        } else {
            qw.and(deptColumn + " = ?", user.deptId);
        }
    }

    private String placeholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        return sb.toString();
    }

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