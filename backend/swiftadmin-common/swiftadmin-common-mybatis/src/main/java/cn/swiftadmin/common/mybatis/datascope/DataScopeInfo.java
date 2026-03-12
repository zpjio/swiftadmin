package cn.swiftadmin.common.mybatis.datascope;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 数据权限信息
 * <p>存储当前用户的数据权限配置</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Data
@Builder
public class DataScopeInfo {

    /** 数据范围：全部数据权限 */
    public static final int SCOPE_ALL = 1;

    /** 数据范围：自定义数据权限 */
    public static final int SCOPE_CUSTOM = 2;

    /** 数据范围：本部门数据权限 */
    public static final int SCOPE_DEPT = 3;

    /** 数据范围：本部门及以下数据权限 */
    public static final int SCOPE_DEPT_AND_CHILD = 4;

    /** 数据范围：仅本人数据权限 */
    public static final int SCOPE_SELF = 5;

    /** 默认数据权限工厂 */
    private static DataScopeFactory defaultFactory;

    /** 用户ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 部门祖级路径（逗号分隔，如 "0,100,101"） */
    private String deptAncestors;

    /** 数据权限范围 */
    private Integer scope;

    /** 自定义数据权限部门ID列表 */
    private List<Long> customDeptIds;

    /**
     * 设置默认数据权限工厂
     */
    public static void setDefaultFactory(DataScopeFactory factory) {
        defaultFactory = factory;
    }

    /**
     * 获取默认数据权限工厂
     */
    public static DataScopeFactory getDefaultFactory() {
        return defaultFactory;
    }

    /**
     * 判断是否有全部数据权限
     */
    public boolean isAllScope() {
        return scope == null || scope == SCOPE_ALL;
    }

    /**
     * 判断是否需要数据权限过滤
     */
    public boolean needFilter() {
        return scope != null && scope != SCOPE_ALL;
    }

}