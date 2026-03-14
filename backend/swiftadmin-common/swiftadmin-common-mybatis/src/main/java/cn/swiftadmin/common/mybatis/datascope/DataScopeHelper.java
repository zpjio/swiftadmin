package cn.swiftadmin.common.mybatis.datascope;

import cn.swiftadmin.common.mybatis.aspect.DataScopeAspect;

/**
 * 数据权限助手类
 * <p>提供跳过数据权限的工具方法</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
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
        return DataScopeAspect.withoutDataScope(supplier);
    }

    /**
     * 在跳过数据权限的情况下执行操作
     *
     * @param runnable 操作
     */
    public static void withoutDataScope(Runnable runnable) {
        DataScopeAspect.withoutDataScope(runnable);
    }

}