package cn.swiftadmin.common.mybatis.datascope;

/**
 * 数据权限工厂接口
 * <p>用于获取当前用户的数据权限信息</p>
 * <p>由业务层实现，通常从 Spring Security 上下文中获取</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@FunctionalInterface
public interface DataScopeFactory {

    /**
     * 获取当前用户的数据权限信息
     *
     * @return 数据权限信息，可能为 null（表示无数据权限限制）
     */
    DataScopeInfo getDataScopeInfo();

}