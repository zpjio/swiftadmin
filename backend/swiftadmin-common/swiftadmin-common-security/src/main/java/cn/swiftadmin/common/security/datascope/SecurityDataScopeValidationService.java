package cn.swiftadmin.common.security.datascope;

import cn.swiftadmin.common.mybatis.service.DataScopeValidationService;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 数据权限校验服务实现
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
public class SecurityDataScopeValidationService implements DataScopeValidationService {

    private static final int SCOPE_SELF = 5;

    @Override
    public boolean canAccessUserData(Long targetUserId) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || user.hasAllDataScope()) {
            return true;
        }
        if (targetUserId != null && targetUserId.equals(user.getUserId())) {
            return true;
        }
        return user.getDataScope() != SCOPE_SELF;
    }

    @Override
    public boolean canAccessDeptData(Long targetDeptId) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || user.hasAllDataScope() || targetDeptId == null) {
            return true;
        }
        return switch (user.getDataScope()) {
            case SCOPE_SELF -> false;
            case 3 -> targetDeptId.equals(user.getDeptId()); // SCOPE_DEPT
            case 4 -> isDeptOrChild(targetDeptId, user);     // SCOPE_DEPT_AND_CHILD
            case 2 -> isCustomDept(targetDeptId, user.getCustomDeptIds()); // SCOPE_CUSTOM
            default -> true;
        };
    }

    @Override
    public boolean canAccessDeptDataWithChildren(Long targetDeptId) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || user.hasAllDataScope() || targetDeptId == null) {
            return true;
        }
        if (targetDeptId.equals(user.getDeptId())) {
            return true;
        }
        return switch (user.getDataScope()) {
            case SCOPE_SELF -> false;
            case 2 -> isCustomDept(targetDeptId, user.getCustomDeptIds());
            default -> isChildOfTargetDept(user.getDeptId(), targetDeptId, user.getDeptAncestors());
        };
    }

    @Override
    public boolean isDataOwner(Long targetUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return currentUserId != null && currentUserId.equals(targetUserId);
    }

    @Override
    public boolean validateDataScope(Long targetUserId, Long targetDeptId) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || user.hasAllDataScope()) {
            return true;
        }
        if (targetUserId != null && targetUserId.equals(user.getUserId())) {
            return true;
        }
        if (targetDeptId == null) {
            return false;
        }
        return switch (user.getDataScope()) {
            case SCOPE_SELF -> false;
            case 3 -> targetDeptId.equals(user.getDeptId());
            case 4 -> isDeptOrChild(targetDeptId, user);
            case 2 -> isCustomDept(targetDeptId, user.getCustomDeptIds());
            default -> true;
        };
    }

    private boolean isDeptOrChild(Long targetDeptId, LoginUser user) {
        if (targetDeptId.equals(user.getDeptId())) {
            return true;
        }
        String ancestors = user.getDeptAncestors();
        return ancestors != null && ancestors.contains(targetDeptId.toString());
    }

    private boolean isChildOfTargetDept(Long currentDeptId, Long targetDeptId, String ancestors) {
        if (currentDeptId.equals(targetDeptId)) {
            return true;
        }
        if (ancestors == null || ancestors.isEmpty()) {
            return false;
        }
        for (String id : ancestors.split(",")) {
            if (targetDeptId.toString().equals(id.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isCustomDept(Long targetDeptId, List<Long> customDeptIds) {
        return !CollectionUtils.isEmpty(customDeptIds) && customDeptIds.contains(targetDeptId);
    }
}