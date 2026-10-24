package cn.swiftadmin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证接口集成测试
 * <p>覆盖：登录成功、登录失败、未认证访问、登出后 Token 失效</p>
 *
 * @author zhangpengju
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 登录成功并返回可用 Token
     */
    @Test
    @DisplayName("账号密码登录成功")
    void loginWithPasswordShouldSucceed() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"password","username":"admin","password":"admin123"}
                                """))
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
                        .content("""
                                {"type":"password","username":"no_such_user_4_test","password":"wrong-password"}
                                """))
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
                        .content("""
                                {"type":"password","username":"admin","password":"admin123"}
                                """))
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

    private String readToken(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }
}
