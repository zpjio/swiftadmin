package cn.swiftadmin.common.mybatis.datascope;

import java.util.function.Supplier;

/**
 * 数据权限管理器
 * <p>参考 MyBatis-Flex TenantManager 设计，提供数据权限控制能力</p>
 * <p>支持跳过数据权限检查、获取当前数据权限信息等</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
public class DataScopeManager {

    /** 数据权限工厂，用于获取当前用户的数据权限信息 */
    private static DataScopeFactory factory;

    /** 跳过数据权限检查的标记（参考 TenantManager.ignoreFlags） */
    private static final ThreadLocal<Boolean> IGNORE_FLAGS = new ThreadLocal<>();

    /** 数据权限上下文，存储当前用户的数据权限信息 */
    private static final ThreadLocal<DataScopeInfo> CONTEXT = new ThreadLocal<>();

    // ==================== 工厂管理 ====================

    /**
     * 设置数据权限工厂
     *
     * @param dataScopeFactory 数据权限工厂
     */
    public static void setFactory(DataScopeFactory dataScopeFactory) {
        factory = dataScopeFactory;
    }

    /**
     * 获取数据权限工厂
     *
     * @return 数据权限工厂
     */
    public static DataScopeFactory getFactory() {
        return factory;
    }

    // ==================== 跳过数据权限（参考 TenantManager.withoutTenantCondition） ====================

    /**
     * 在跳过数据权限检查的情况下执行操作
     * <p>参考 TenantManager.withoutTenantCondition 设计</p>
     *
     * @param supplier 要执行的操作
     * @param <T>      返回类型
     * @return 操作结果
     */
    public static <T> T withoutDataScope(Supplier<T> supplier) {
        IGNORE_FLAGS.set(Boolean.TRUE);
        try {
            return supplier.get();
        } finally {
            IGNORE_FLAGS.remove();
        }
    }

    /**
     * 在跳过数据权限检查的情况下执行操作（无返回值）
     * <p>参考 TenantManager.withoutTenantCondition(Runnable) 设计</p>
     *
     * @param runnable 要执行的操作
     */
    public static void withoutDataScope(Runnable runnable) {
        IGNORE_FLAGS.set(Boolean.TRUE);
        try {
            runnable.run();
        } finally {
            IGNORE_FLAGS.remove();
        }
    }

    /**
     * 判断是否跳过数据权限检查
     * <p>参考 TenantManager.isIgnoreTenantCondition 设计</p>
     *
     * @return true 表示跳过
     */
    public static boolean isIgnoreDataScope() {
        return Boolean.TRUE.equals(IGNORE_FLAGS.get());
    }

    // ==================== 数据权限上下文管理 ====================

    /**
     * 设置当前数据权限信息
     *
     * @param dataScopeInfo 数据权限信息
     */
    public static void setDataScopeInfo(DataScopeInfo dataScopeInfo) {
        CONTEXT.set(dataScopeInfo);
    }

    /**
     * 获取当前数据权限信息
     * <p>优先从 ThreadLocal 获取，其次从工厂获取</p>
     *
     * @return 数据权限信息，可能为 null
     */
    public static DataScopeInfo getDataScopeInfo() {
        DataScopeInfo info = CONTEXT.get();
        if (info == null) {
            // 优先使用设置的工厂，其次使用默认工厂
            if (factory != null) {
                info = factory.getDataScopeInfo();
            } else if (DataScopeInfo.getDefaultFactory() != null) {
                info = DataScopeInfo.getDefaultFactory().getDataScopeInfo();
            }
        }
        return info;
    }

    /**
     * 清除当前数据权限信息
     */
    public static void clearDataScopeInfo() {
        CONTEXT.remove();
    }

    // ==================== 便捷方法 ====================

    /**
     * 判断当前用户是否有全部数据权限
     *
     * @return true 表示有全部数据权限
     */
    public static boolean hasAllDataScope() {
        DataScopeInfo info = getDataScopeInfo();
        return info == null || info.isAllScope();
    }

    /**
     * 判断是否需要数据权限过滤
     *
     * @return true 表示需要过滤
     */
    public static boolean needDataScopeFilter() {
        if (isIgnoreDataScope()) {
            return false;
        }
        DataScopeInfo info = getDataScopeInfo();
        return info != null && info.needFilter();
    }

}