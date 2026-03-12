package cn.swiftadmin.common.mybatis.handler;

import cn.swiftadmin.common.mybatis.annotation.DataScope;
import cn.swiftadmin.common.mybatis.aspect.DataScopeAspect;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;

/**
 * 数据权限处理器
 * <p>根据用户的数据权限范围，动态拼接 SQL 条件</p>
 * <p>支持部门祖级路径实现本部门及以下数据权限</p>
 * <p>使用参数化查询，避免 SQL 注入风险</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public class DataScopeHandler {

    /** 数据范围：全部数据权限 */
    public static final int DATA_SCOPE_ALL = 1;

    /** 数据范围：自定义数据权限 */
    public static final int DATA_SCOPE_CUSTOM = 2;

    /** 数据范围：本部门数据权限 */
    public static final int DATA_SCOPE_DEPT = 3;

    /** 数据范围：本部门及以下数据权限 */
    public static final int DATA_SCOPE_DEPT_AND_CHILD = 4;

    /** 数据范围：仅本人数据权限 */
    public static final int DATA_SCOPE_SELF = 5;

    private final Long userId;
    private final Long deptId;
    private final String deptAncestors;
    private final Integer dataScope;
    private final List<Long> customDeptIds;

    public DataScopeHandler(Long userId, Long deptId, Integer dataScope, List<Long> customDeptIds) {
        this(userId, deptId, null, dataScope, customDeptIds);
    }

    public DataScopeHandler(Long userId, Long deptId, String deptAncestors,
                            Integer dataScope, List<Long> customDeptIds) {
        this.userId = userId;
        this.deptId = deptId;
        this.deptAncestors = deptAncestors;
        this.dataScope = dataScope;
        this.customDeptIds = customDeptIds;
    }

    /**
     * 应用数据权限过滤
     * <p>使用参数化查询，避免 SQL 注入</p>
     */
    @SuppressWarnings("unchecked")
    public void applyDataScope(QueryWrapper queryWrapper) {
        DataScopeAspect.DataScopeContext context = DataScopeAspect.getContext();
        if (context == null) {
            return;
        }

        DataScope dataScopeAnnotation = context.getDataScope();
        if (dataScopeAnnotation == null) {
            return;
        }

        String deptAlias = dataScopeAnnotation.deptAlias();
        String userAlias = dataScopeAnnotation.userAlias();
        String deptIdField = dataScopeAnnotation.deptIdField();
        String userIdField = dataScopeAnnotation.userIdField();

        switch (this.dataScope != null ? this.dataScope : 0) {
            case DATA_SCOPE_ALL:
                break;

            case DATA_SCOPE_CUSTOM:
                if (customDeptIds != null && !customDeptIds.isEmpty()) {
                    String deptColumn = deptAlias + "." + deptIdField;
                    queryWrapper.and(deptColumn + " IN (" + buildPlaceholders(customDeptIds.size()) + ")",
                            customDeptIds.toArray());
                }
                break;

            case DATA_SCOPE_DEPT:
                queryWrapper.and(deptAlias + "." + deptIdField + " = ?", deptId);
                break;

            case DATA_SCOPE_DEPT_AND_CHILD:
                applyDeptAndChildScope(queryWrapper, deptAlias, deptIdField);
                break;

            case DATA_SCOPE_SELF:
                queryWrapper.and(userAlias + "." + userIdField + " = ?", userId);
                break;

            default:
                break;
        }
    }

    /**
     * 应用本部门及以下数据权限
     */
    @SuppressWarnings("unchecked")
    private void applyDeptAndChildScope(QueryWrapper queryWrapper, String deptAlias, String deptIdField) {
        String deptColumn = deptAlias + "." + deptIdField;

        if (deptAncestors != null && !deptAncestors.isEmpty()) {
            String ancestorPattern = deptAncestors + "," + deptId;

            // 使用原始 SQL 方式构建嵌套条件
            String condition = "(" + deptColumn + " = ? OR "
                    + deptAlias + ".ancestors LIKE ? OR "
                    + deptAlias + ".ancestors LIKE ?)";
            queryWrapper.and(condition, deptId, ancestorPattern + ",%", ancestorPattern);
        } else {
            queryWrapper.and(deptColumn + " = ?", deptId);
        }
    }

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

    public boolean hasDataScope() {
        return dataScope != null && dataScope != DATA_SCOPE_ALL;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public Integer getDataScope() {
        return dataScope;
    }

    public List<Long> getCustomDeptIds() {
        return customDeptIds;
    }

    public String getDeptAncestors() {
        return deptAncestors;
    }

}