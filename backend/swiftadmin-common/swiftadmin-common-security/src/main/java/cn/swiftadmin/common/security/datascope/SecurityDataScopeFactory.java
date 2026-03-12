package cn.swiftadmin.common.security.datascope;

import cn.swiftadmin.common.mybatis.datascope.DataScopeFactory;
import cn.swiftadmin.common.mybatis.datascope.DataScopeInfo;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 数据权限工厂实现
 * <p>从 Spring Security 上下文中获取当前用户的数据权限信息</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityDataScopeFactory implements DataScopeFactory {

    private final PermissionCacheService permissionCacheService;

    @PostConstruct
    public void init() {
        DataScopeInfo.setDefaultFactory(this);
        log.info("数据权限工厂初始化完成");
    }

    @Override
    public DataScopeInfo getDataScopeInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof LoginUser)) {
            return null;
        }

        LoginUser loginUser = (LoginUser) principal;

        // 超级管理员拥有全部数据权限
        if (loginUser.isSuperAdmin()) {
            return DataScopeInfo.builder()
                    .userId(loginUser.getUserId())
                    .deptId(loginUser.getDeptId())
                    .scope(DataScopeInfo.SCOPE_ALL)
                    .build();
        }

        return DataScopeInfo.builder()
                .userId(loginUser.getUserId())
                .deptId(loginUser.getDeptId())
                .deptAncestors(loginUser.getDeptAncestors())
                .scope(loginUser.getDataScope())
                .customDeptIds(loginUser.getCustomDeptIds())
                .build();
    }

}