package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.mybatis.service.UserInfoQueryService;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    @Override
    public Long getDeptIdByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userService.getById(userId);
        return user != null ? user.getDeptId() : null;
    }

    @Override
    public String getUsernameByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userService.getById(userId);
        return user != null ? user.getUsername() : null;
    }

}