package cn.swiftadmin;

import cn.swiftadmin.system.api.service.PermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限服务集成测试
 * <p>基于初始化数据校验超级管理员的角色与菜单权限解析</p>
 *
 * @author zhangpengju
 */
@SpringBootTest
class PermissionIntegrationTest {

    /** 初始化 SQL 中的超级管理员用户 ID */
    private static final long ADMIN_USER_ID = 1L;

    @Autowired
    private PermissionService permissionService;

    @Test
    @DisplayName("超级管理员拥有角色与核心菜单权限")
    void adminShouldHaveRolesAndPermissions() {
        assertThat(permissionService.getRolesByUserId(ADMIN_USER_ID))
                .contains("admin");

        assertThat(permissionService.getPermissionsByUserId(ADMIN_USER_ID))
                .contains("system:user:list", "system:user:add", "system:user:edit", "system:user:delete");
    }

    @Test
    @DisplayName("hasPermission / hasRole 判断正确")
    void hasPermissionAndRoleShouldWork() {
        assertThat(permissionService.hasPermission(ADMIN_USER_ID, "system:user:list")).isTrue();
        assertThat(permissionService.hasRole(ADMIN_USER_ID, "admin")).isTrue();

        // 不存在或未分配的权限/角色应返回 false
        assertThat(permissionService.hasPermission(ADMIN_USER_ID, "system:not-exist:permission")).isFalse();
        assertThat(permissionService.hasRole(ADMIN_USER_ID, "not-exist-role")).isFalse();
    }

    @Test
    @DisplayName("不存在的用户没有任何权限")
    void unknownUserShouldHaveNoPermission() {
        long unknownUserId = 999_999_999L;
        assertThat(permissionService.getPermissionsByUserId(unknownUserId)).isEmpty();
        assertThat(permissionService.hasPermission(unknownUserId, "system:user:list")).isFalse();
    }
}
