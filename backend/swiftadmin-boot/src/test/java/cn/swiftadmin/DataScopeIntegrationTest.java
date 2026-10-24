package cn.swiftadmin;

import cn.swiftadmin.common.security.datascope.SecurityDataScopeValidationService;
import cn.swiftadmin.common.security.domain.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据权限集成测试
 * <p>基于 {@link SecurityDataScopeValidationService} 校验不同数据范围下的访问控制</p>
 *
 * @author zhangpengju
 */
@SpringBootTest
class DataScopeIntegrationTest {

    /** 数据范围：全部 */
    private static final int SCOPE_ALL = 1;
    /** 数据范围：自定义 */
    private static final int SCOPE_CUSTOM = 2;
    /** 数据范围：本部门 */
    private static final int SCOPE_DEPT = 3;
    /** 数据范围：仅本人 */
    private static final int SCOPE_SELF = 5;

    @Autowired
    private SecurityDataScopeValidationService validationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("超级管理员可访问任意部门与用户数据")
    void superAdminCanAccessEverything() {
        authenticate(1L, 100L, SCOPE_ALL, true);

        assertThat(validationService.canAccessDeptData(999L)).isTrue();
        assertThat(validationService.canAccessUserData(888L)).isTrue();
        assertThat(validationService.validateDataScope(888L, 999L)).isTrue();
    }

    @Test
    @DisplayName("本部门范围只能访问本部门")
    void deptScopeOnlyAllowsOwnDept() {
        authenticate(1001L, 100L, SCOPE_DEPT, false);

        assertThat(validationService.canAccessDeptData(100L)).isTrue();
        assertThat(validationService.canAccessDeptData(101L)).isFalse();
        assertThat(validationService.validateDataScope(2001L, 100L)).isTrue();
        assertThat(validationService.validateDataScope(2001L, 101L)).isFalse();
    }

    @Test
    @DisplayName("仅本人范围只能访问自己的数据")
    void selfScopeOnlyAllowsOwnData() {
        authenticate(1001L, 100L, SCOPE_SELF, false);

        assertThat(validationService.canAccessUserData(1001L)).isTrue();
        assertThat(validationService.canAccessUserData(1002L)).isFalse();
        assertThat(validationService.canAccessDeptData(100L)).isFalse();
    }

    @Test
    @DisplayName("自定义范围只能访问指定部门")
    void customScopeOnlyAllowsConfiguredDepts() {
        LoginUser user = buildUser(1001L, 100L, SCOPE_CUSTOM, false);
        user.setCustomDeptIds(List.of(200L, 201L));
        setAuthentication(user);

        assertThat(validationService.canAccessDeptData(200L)).isTrue();
        assertThat(validationService.canAccessDeptData(201L)).isTrue();
        assertThat(validationService.canAccessDeptData(202L)).isFalse();
        assertThat(validationService.validateDataScope(2001L, 200L)).isTrue();
        assertThat(validationService.validateDataScope(2001L, 202L)).isFalse();
    }

    private void authenticate(Long userId, Long deptId, int dataScope, boolean superAdmin) {
        setAuthentication(buildUser(userId, deptId, dataScope, superAdmin));
    }

    private LoginUser buildUser(Long userId, Long deptId, int dataScope, boolean superAdmin) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUsername("test-user-" + userId);
        user.setDeptId(deptId);
        user.setDataScope(dataScope);
        user.setSuperAdmin(superAdmin);
        return user;
    }

    private void setAuthentication(LoginUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
