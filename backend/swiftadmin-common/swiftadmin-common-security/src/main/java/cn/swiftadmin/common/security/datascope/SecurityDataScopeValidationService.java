package cn.swiftadmin.common.security.datascope;

import cn.swiftadmin.common.mybatis.datascope.DataScopeInfo;
import cn.swiftadmin.common.mybatis.datascope.DataScopeManager;
import cn.swiftadmin.common.mybatis.service.DataScopeValidationService;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 数据权限校验服务实现
 * <p>基于当前用户的数据权限范围进行校验</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
public class SecurityDataScopeValidationService implements DataScopeValidationService {

    @Override
    public boolean canAccessUserData(Long targetUserId) {
        DataScopeInfo info = DataScopeManager.getDataScopeInfo();
        if (info == null || info.isAllScope()) {
            return true;
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        // 本人数据
        if (targetUserId != null && targetUserId.equals(currentUserId)) {
            return true;
        }

        // 根据数据权限范围判断
        return switch (info.getScope()) {
            case DataScopeInfo.SCOPE_SELF -> false; // 仅本人，且不是本人数据
            case DataScopeInfo.SCOPE_DEPT, DataScopeInfo.SCOPE_DEPT_AND_CHILD -> true; // 部门权限需要进一步检查部门
            case DataScopeInfo.SCOPE_CUSTOM -> true; // 自定义权限需要检查部门
            default -> true;
        };
    }

    @Override
    public boolean canAccessDeptData(Long targetDeptId) {
        DataScopeInfo info = DataScopeManager.getDataScopeInfo();
        if (info == null || info.isAllScope()) {
            return true;
        }

        if (targetDeptId == null) {
            return true;
        }

        return switch (info.getScope()) {
            case DataScopeInfo.SCOPE_SELF -> false;
            case DataScopeInfo.SCOPE_DEPT -> targetDeptId.equals(info.getDeptId());
            case DataScopeInfo.SCOPE_DEPT_AND_CHILD -> isDeptOrChild(targetDeptId, info);
            case DataScopeInfo.SCOPE_CUSTOM -> isCustomDept(targetDeptId, info.getCustomDeptIds());
            default -> true;
        };
    }

    @Override
    public boolean canAccessDeptDataWithChildren(Long targetDeptId) {
        DataScopeInfo info = DataScopeManager.getDataScopeInfo();
        if (info == null || info.isAllScope()) {
            return true;
        }

        if (targetDeptId == null) {
            return true;
        }

        return switch (info.getScope()) {
            case DataScopeInfo.SCOPE_SELF -> false;
            case DataScopeInfo.SCOPE_DEPT, DataScopeInfo.SCOPE_DEPT_AND_CHILD -> {
                // 目标部门是自己部门或者是子部门
                if (targetDeptId.equals(info.getDeptId())) {
                    yield true;
                }
                // 检查目标部门是否在当前部门树下
                yield isChildOfTargetDept(info.getDeptId(), targetDeptId, info.getDeptAncestors());
            }
            case DataScopeInfo.SCOPE_CUSTOM -> isCustomDept(targetDeptId, info.getCustomDeptIds());
            default -> true;
        };
    }

    @Override
    public boolean isDataOwner(Long targetUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return currentUserId != null && currentUserId.equals(targetUserId);
    }

    @Override
    public boolean validateDataScope(Long targetUserId, Long targetDeptId) {
        DataScopeInfo info = DataScopeManager.getDataScopeInfo();
        if (info == null || info.isAllScope()) {
            return true;
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        // 本人数据直接允许
        if (targetUserId != null && targetUserId.equals(currentUserId)) {
            return true;
        }

        return switch (info.getScope()) {
            case DataScopeInfo.SCOPE_SELF -> false; // 仅本人
            case DataScopeInfo.SCOPE_DEPT -> {
                if (targetDeptId == null) {
                    yield false;
                }
                yield targetDeptId.equals(info.getDeptId());
            }
            case DataScopeInfo.SCOPE_DEPT_AND_CHILD -> {
                if (targetDeptId == null) {
                    yield false;
                }
                yield isDeptOrChild(targetDeptId, info);
            }
            case DataScopeInfo.SCOPE_CUSTOM -> {
                if (targetDeptId == null) {
                    yield false;
                }
                yield isCustomDept(targetDeptId, info.getCustomDeptIds());
            }
            default -> true;
        };
    }

    /**
     * 判断目标部门是否是当前用户的部门或其子部门
     */
    private boolean isDeptOrChild(Long targetDeptId, DataScopeInfo info) {
        if (targetDeptId.equals(info.getDeptId())) {
            return true;
        }

        String ancestors = info.getDeptAncestors();
        if (ancestors == null || ancestors.isEmpty()) {
            return false;
        }

        // 目标部门是当前部门的子部门
        // ancestors 格式: "0,100,101"，表示根节点到当前节点的路径
        return ancestors.contains(targetDeptId.toString());
    }

    /**
     * 判断当前部门是否是目标部门的子部门
     * 用于判断是否有权限操作目标部门及其子部门的数据
     */
    private boolean isChildOfTargetDept(Long currentDeptId, Long targetDeptId, String ancestors) {
        if (currentDeptId.equals(targetDeptId)) {
            return true;
        }

        if (ancestors == null || ancestors.isEmpty()) {
            return false;
        }

        // 如果目标部门在祖级路径中，说明当前部门是目标部门的子部门
        String[] ancestorIds = ancestors.split(",");
        for (String ancestorId : ancestorIds) {
            if (targetDeptId.toString().equals(ancestorId.trim())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断部门是否在自定义部门列表中
     */
    private boolean isCustomDept(Long targetDeptId, List<Long> customDeptIds) {
        if (CollectionUtils.isEmpty(customDeptIds)) {
            return false;
        }
        return customDeptIds.contains(targetDeptId);
    }
}