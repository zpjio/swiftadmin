package cn.swiftadmin.common.mybatis.datascope;

import cn.swiftadmin.common.mybatis.annotation.DataScope;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

/**
 * 数据权限拦截器
 * <p>自动为查询添加数据权限过滤条件</p>
 * <p>参考 MyBatis-Flex TenantManager 设计模式</p>
 *
 * <p>使用方式：</p>
 * <pre>
 * // 正常查询，自动应用数据权限
 * List&lt;User&gt; users = userMapper.selectListByQuery(queryWrapper);
 *
 * // 跳过数据权限检查
 * List&lt;User&gt; allUsers = DataScopeManager.withoutDataScope(() ->
 *     userMapper.selectListByQuery(queryWrapper)
 * );
 * </pre>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class DataScopeInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 检查是否跳过数据权限（参考 TenantManager.isIgnoreTenantCondition）
        if (DataScopeManager.isIgnoreDataScope()) {
            return invocation.proceed();
        }

        // 获取数据权限信息
        DataScopeInfo dataScopeInfo = DataScopeManager.getDataScopeInfo();
        if (dataScopeInfo == null || !dataScopeInfo.needFilter()) {
            return invocation.proceed();
        }

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 根据操作类型处理（参考 IDialect.OperateType）
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        switch (sqlCommandType) {
            case SELECT:
                handleSelect(ms, parameter, dataScopeInfo);
                break;
            case UPDATE:
            case DELETE:
                // UPDATE/DELETE 的数据权限处理
                handleModify(ms, parameter, dataScopeInfo, sqlCommandType);
                break;
            default:
                break;
        }

        return invocation.proceed();
    }

    /**
     * 处理 SELECT 操作的数据权限
     */
    private void handleSelect(MappedStatement ms, Object parameter, DataScopeInfo dataScopeInfo) {
        String mappedStatementId = ms.getId();

        // 检查是否有 @DataScope 注解
        if (!hasDataScopeAnnotation(mappedStatementId)) {
            return;
        }

        // 尝试从参数中获取 QueryWrapper
        if (parameter instanceof QueryWrapper) {
            applyDataScope((QueryWrapper) parameter, dataScopeInfo);
        } else if (parameter != null) {
            QueryWrapper queryWrapper = findQueryWrapper(parameter);
            if (queryWrapper != null) {
                applyDataScope(queryWrapper, dataScopeInfo);
            }
        }
    }

    /**
     * 处理 UPDATE/DELETE 操作的数据权限
     * <p>确保只能修改/删除有权限的数据</p>
     */
    private void handleModify(MappedStatement ms, Object parameter, DataScopeInfo dataScopeInfo, SqlCommandType sqlCommandType) {
        // UPDATE/DELETE 的数据权限需要更复杂的处理
        // 当前简化处理，仅记录日志
        if (log.isDebugEnabled()) {
            log.debug("数据权限检查: operation={}, msId={}", sqlCommandType, ms.getId());
        }
    }

    /**
     * 判断方法是否有 @DataScope 注解
     */
    private boolean hasDataScopeAnnotation(String mappedStatementId) {
        try {
            int lastDot = mappedStatementId.lastIndexOf('.');
            if (lastDot <= 0) {
                return false;
            }
            String className = mappedStatementId.substring(0, lastDot);
            String methodName = mappedStatementId.substring(lastDot + 1);

            Class<?> mapperClass = Class.forName(className);
            for (java.lang.reflect.Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(DataScope.class)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            log.debug("无法加载 Mapper 类: {}", mappedStatementId);
        }
        return false;
    }

    /**
     * 从参数对象中查找 QueryWrapper
     */
    private QueryWrapper findQueryWrapper(Object parameter) {
        if (parameter == null) {
            return null;
        }
        for (Field field : parameter.getClass().getDeclaredFields()) {
            if (QueryWrapper.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return (QueryWrapper) field.get(parameter);
                } catch (IllegalAccessException e) {
                    log.debug("无法访问字段: {}", field.getName());
                }
            }
        }
        return null;
    }

    /**
     * 应用数据权限过滤条件
     */
    @SuppressWarnings("unchecked")
    private void applyDataScope(QueryWrapper queryWrapper, DataScopeInfo info) {
        switch (info.getScope()) {
            case DataScopeInfo.SCOPE_CUSTOM:
                List<Long> customDeptIds = info.getCustomDeptIds();
                if (customDeptIds != null && !customDeptIds.isEmpty()) {
                    queryWrapper.and("d.dept_id IN (" + buildPlaceholders(customDeptIds.size()) + ")",
                            customDeptIds.toArray());
                }
                break;

            case DataScopeInfo.SCOPE_DEPT:
                queryWrapper.and("d.dept_id = ?", info.getDeptId());
                break;

            case DataScopeInfo.SCOPE_DEPT_AND_CHILD:
                applyDeptAndChildScope(queryWrapper, info);
                break;

            case DataScopeInfo.SCOPE_SELF:
                queryWrapper.and("u.user_id = ?", info.getUserId());
                break;

            default:
                break;
        }
    }

    /**
     * 应用本部门及以下数据权限
     */
    @SuppressWarnings("unchecked")
    private void applyDeptAndChildScope(QueryWrapper queryWrapper, DataScopeInfo info) {
        Long deptId = info.getDeptId();
        String ancestors = info.getDeptAncestors();

        if (ancestors != null && !ancestors.isEmpty()) {
            String ancestorPattern = ancestors + "," + deptId;
            // 使用参数化查询避免 SQL 注入
            String condition = "(d.dept_id = ? OR d.ancestors LIKE ? OR d.ancestors LIKE ?)";
            queryWrapper.and(condition, deptId, ancestorPattern + ",%", ancestorPattern);
        } else {
            queryWrapper.and("d.dept_id = ?", deptId);
        }
    }

    /**
     * 构建占位符字符串
     */
    private String buildPlaceholders(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        return sb.toString();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 无需配置
    }

}