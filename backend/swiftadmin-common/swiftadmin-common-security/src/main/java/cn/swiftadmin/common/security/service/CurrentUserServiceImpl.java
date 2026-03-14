package cn.swiftadmin.common.security.service;

import cn.swiftadmin.common.core.service.CurrentUserService;
import cn.swiftadmin.common.security.util.SecurityUtils;
import org.springframework.stereotype.Service;

/**
 * 当前用户上下文服务实现
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    @Override
    public Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    @Override
    public String getCurrentUsername() {
        return SecurityUtils.getCurrentUsername();
    }
}