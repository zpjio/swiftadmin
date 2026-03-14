package cn.swiftadmin.common.security.datascope;

import cn.swiftadmin.common.mybatis.aspect.DataScopeAspect;
import cn.swiftadmin.common.mybatis.aspect.DataScopeAspect.DataScopeUser;
import cn.swiftadmin.common.mybatis.aspect.DataScopeAspect.SecurityContextHolder;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.common.security.util.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring Security 数据上下文持有者
 *
 * @author zhangpengju
 * @since 2026-03-14
 */
@Slf4j
@Component
public class SpringSecurityContextHolder implements SecurityContextHolder {

    @PostConstruct
    public void init() {
        DataScopeAspect.setContextHolder(this);
        log.info("数据权限上下文持有者初始化完成");
    }

    @Override
    public DataScopeUser getCurrentUser() {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null) {
            log.debug("getCurrentUser: LoginUser is null");
            return null;
        }
        if (user.isSuperAdmin()) {
            log.debug("getCurrentUser: user is superAdmin, skip filter");
            return null;
        }
        if (user.hasAllDataScope()) {
            log.debug("getCurrentUser: user hasAllDataScope, skip filter");
            return null;
        }

        DataScopeUser dsUser = new DataScopeUser();
        dsUser.setUserId(user.getUserId());
        dsUser.setDeptId(user.getDeptId());
        dsUser.setDeptAncestors(user.getDeptAncestors());
        dsUser.setScope(user.getDataScope());

        List<Long> customDeptIds = user.getCustomDeptIds();
        if (customDeptIds != null && !customDeptIds.isEmpty()) {
            dsUser.setCustomDeptIds(customDeptIds);
        }

        log.debug("getCurrentUser: userId={}, deptId={}, deptAncestors={}, scope={}, customDeptIds={}",
                user.getUserId(), user.getDeptId(), user.getDeptAncestors(),
                user.getDataScope(), customDeptIds);

        return dsUser;
    }
}