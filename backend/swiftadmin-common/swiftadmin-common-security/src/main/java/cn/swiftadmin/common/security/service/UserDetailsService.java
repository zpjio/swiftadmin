package cn.swiftadmin.common.security.service;

import cn.swiftadmin.common.security.domain.LoginUser;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 用户详情服务接口
 * <p>扩展 Spring Security 的 UserDetailsService，支持按 ID 加载用户和刷新权限</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
public interface UserDetailsService extends org.springframework.security.core.userdetails.UserDetailsService {

    @Override
    LoginUser loadUserByUsername(String username) throws UsernameNotFoundException;

    /**
     * 根据用户 ID 加载用户信息
     *
     * @param userId 用户 ID
     * @return 登录用户信息
     */
    LoginUser loadUserByUserId(Long userId);

    /**
     * 刷新用户权限缓存
     *
     * @param userId 用户 ID
     */
    void refreshUserPermissions(Long userId);

}
