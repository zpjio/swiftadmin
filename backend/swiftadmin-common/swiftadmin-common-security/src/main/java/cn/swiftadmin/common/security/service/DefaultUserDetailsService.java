package cn.swiftadmin.common.security.service;

import cn.swiftadmin.common.security.domain.LoginUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 默认用户详情服务实现
 * <p>当项目中未定义自定义 UserDetailsService 时生效，返回 null 表示需要用户自行实现</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Service
@ConditionalOnMissingBean(UserDetailsService.class)
public class DefaultUserDetailsService implements UserDetailsService {

    @Override
    public LoginUser loadUserByUsername(String username) {
        return null;
    }

    @Override
    public LoginUser loadUserByUserId(Long userId) {
        return null;
    }

    @Override
    public void refreshUserPermissions(Long userId) {
    }

}
