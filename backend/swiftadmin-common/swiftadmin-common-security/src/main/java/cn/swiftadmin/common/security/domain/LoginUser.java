package cn.swiftadmin.common.security.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户信息
 * <p>实现 Spring Security 的 UserDetails 接口，用于存储认证用户信息和权限</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Data
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 密码（加密） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 部门 ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 部门祖级路径（逗号分隔） */
    private String deptAncestors;

    /** 是否超级管理员 */
    private boolean superAdmin;

    /** 状态：1=正常，0=禁用 */
    private Integer status;

    /** 权限标识集合 */
    private Set<String> permissions;

    /** 角色标识集合 */
    private Set<String> roles;

    /** 数据权限范围：1=全部，2=自定义，3=本部门，4=本部门及以下，5=仅本人 */
    private Integer dataScope;

    /** 自定义数据权限部门ID列表 */
    private List<Long> customDeptIds;

    public LoginUser() {
    }

    public LoginUser(Long userId, String username, String password, Integer status) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.status = status;
    }

    /**
     * 判断是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return superAdmin;
    }

    /**
     * 判断是否拥有全部数据权限
     */
    public boolean hasAllDataScope() {
        return superAdmin || (dataScope != null && dataScope == 1);
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new java.util.HashSet<>();

        if (roles != null) {
            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet()));
        }

        if (permissions != null) {
            authorities.addAll(permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet()));
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

}