package cn.swiftadmin.common.mybatis.datascope;

import cn.swiftadmin.common.mybatis.handler.DataScopeHandler;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据权限助手类
 * <p>提供便捷的数据权限操作方法</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Slf4j
public final class DataScopeHelper {

    private DataScopeHelper() {
    }

    /**
     * 在跳过数据权限的情况下执行查询
     *
     * @param supplier 查询操作
     * @param <T>      返回类型
     * @return 查询结果
     */
    public static <T> T withoutDataScope(java.util.function.Supplier<T> supplier) {
        return DataScopeManager.withoutDataScope(supplier);
    }

    /**
     * 在跳过数据权限的情况下执行操作
     *
     * @param runnable 操作
     */
    public static void withoutDataScope(Runnable runnable) {
        DataScopeManager.withoutDataScope(runnable);
    }

    /**
     * 手动应用数据权限到 QueryWrapper
     * <p>当自动拦截器无法处理时使用</p>
     *
     * @param queryWrapper   查询条件
     * @param dataScopeInfo  数据权限信息
     */
    public static void applyDataScope(QueryWrapper queryWrapper, DataScopeInfo dataScopeInfo) {
        if (queryWrapper == null || dataScopeInfo == null || !dataScopeInfo.needFilter()) {
            return;
        }

        DataScopeHandler handler = new DataScopeHandler(
                dataScopeInfo.getUserId(),
                dataScopeInfo.getDeptId(),
                dataScopeInfo.getDeptAncestors(),
                dataScopeInfo.getScope(),
                dataScopeInfo.getCustomDeptIds()
        );

        handler.applyDataScope(queryWrapper);
    }

    /**
     * 判断当前是否跳过数据权限
     *
     * @return true 表示跳过
     */
    public static boolean isIgnoreDataScope() {
        return DataScopeManager.isIgnoreDataScope();
    }

    /**
     * 获取当前数据权限信息
     *
     * @return 数据权限信息
     */
    public static DataScopeInfo getDataScopeInfo() {
        return DataScopeManager.getDataScopeInfo();
    }

}