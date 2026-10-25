package cn.swiftadmin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证接口集成测试
 * <p>覆盖：登录成功、登录失败、未认证访问、登出后 Token 失效</p>
 * <p>使用独立的测试用户，避免依赖或修改默认管理员密码（默认密码首次登录会被要求强制修改）</p>
 *
 * @author zhangpengju
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest extends IntegrationTestBase {

    /** 测试用户 ID，避免与初始化数据冲突 */
    private static final long TEST_USER_ID = 900_000_001L;
    private static final String TEST_USERNAME = "it_auth_user";
    private static final String TEST_PASSWORD = "ItAuth@12345";
    /** 初始化 SQL 中的系统管理员角色，拥有 system:user:list 权限 */
    private static final long TEST_ROLE_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpTestUser() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanUpTestUser();
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password, nickname, status, dept_id, is_super,
                                      password_update_time, force_change_password, login_fail_count, deleted)
                VALUES (?, ?, ?, ?, 1, 1, 0, NOW(), 0, 0, 0)
                """,
                TEST_USER_ID, TEST_USERNAME, passwordEncoder.encode(TEST_PASSWORD), "集成测试用户");
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                TEST_USER_ID, TEST_ROLE_ID);
    }

    @AfterEach
    void cleanUpTestUser() {
        jdbcTemplate.update("""
                DELETE FROM sys_user_role WHERE user_id IN
                (SELECT id FROM sys_user WHERE id = ? OR username = ?)
                """, TEST_USER_ID, TEST_USERNAME);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ? OR username = ?",
                TEST_USER_ID, TEST_USERNAME);
    }

    /**
     * 登录成功并返回可用 Token
     */
    @Test
    @DisplayName("账号密码登录成功")
    void loginWithPasswordShouldSucceed() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String token = readToken(result);
        assertThat(token).isNotBlank();

        // 使用登录返回的 Token 访问受保护接口应成功
        mockMvc.perform(get("/api/system/user/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 登录失败不应签发 Token
     */
    @Test
    @DisplayName("错误账号登录失败")
    void loginWithUnknownUserShouldFail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("no_such_user_4_test", "wrong-password")))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(0)))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 未携带 Token 访问受保护接口应返回 401
     */
    @Test
    @DisplayName("未认证访问受保护接口返回 401")
    void protectedApiWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/system/user/page"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 修改密码接口必须要求认证（回归测试：曾存在未认证即可调用的漏洞）
     */
    @Test
    @DisplayName("未认证修改密码返回 401")
    void changePasswordWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/password/change")
                        .param("oldPassword", "admin123")
                        .param("newPassword", "Hacked@123456"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 登出后原 Token 应立即失效
     */
    @Test
    @DisplayName("登出后 Token 失效")
    void logoutShouldInvalidateToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(TEST_USERNAME, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String token = readToken(result);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 已登出的 Token 再次访问应被拒绝
        mockMvc.perform(get("/api/system/user/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String loginBody(String username, String password) {
        return """
                {"type":"password","username":"%s","password":"%s"}
                """.formatted(username, password);
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }
}
