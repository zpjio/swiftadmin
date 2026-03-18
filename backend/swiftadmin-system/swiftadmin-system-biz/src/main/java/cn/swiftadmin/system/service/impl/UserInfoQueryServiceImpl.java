package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.mybatis.service.UserInfoQueryService;
import cn.swiftadmin.system.api.entity.SysDept;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import cn.swiftadmin.system.mapper.SysDeptMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户信息查询服务实现
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoQueryServiceImpl implements UserInfoQueryService {

    private final UserService userService;
    private final SysDeptMapper deptMapper;

    @Override
    public Long getDeptIdByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userService.getById(userId);
        return user != null ? user.getDeptId() : null;
    }

    @Override
    public Map<Long, Long> getDeptIdsByUserIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<SysUser> users = userService.listByIds(userIds);
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream()
                .filter(u -> u.getDeptId() != null)
                .collect(Collectors.toMap(SysUser::getId, SysUser::getDeptId, (v1, v2) -> v1));
    }

    @Override
    public String getUsernameByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userService.getById(userId);
        return user != null ? user.getUsername() : null;
    }

    @Override
    public String getDeptAncestors(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept dept = deptMapper.selectOneById(deptId);
        return dept != null ? dept.getAncestors() : null;
    }

    @Override
    public boolean isChildDeptOrSelf(Long targetDeptId, Long currentDeptId) {
        if (targetDeptId == null || currentDeptId == null) {
            return false;
        }
        // 相同部门
        if (targetDeptId.equals(currentDeptId)) {
            return true;
        }
        // 查询目标部门的 ancestors，判断是否包含当前部门ID
        String ancestors = getDeptAncestors(targetDeptId);
        if (!StringUtils.hasText(ancestors)) {
            return false;
        }
        for (String id : ancestors.split(",")) {
            if (currentDeptId.toString().equals(id.trim())) {
                return true;
            }
        }
        return false;
    }

}