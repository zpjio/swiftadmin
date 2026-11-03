package cn.swiftadmin;

import cn.swiftadmin.common.core.file.FileInfo;
import cn.swiftadmin.common.core.file.FileStorageException;
import cn.swiftadmin.common.core.file.FileUploadService;
import cn.swiftadmin.common.core.file.LocalFileStorageStrategy;
import cn.swiftadmin.common.core.file.StoragePaths;
import cn.swiftadmin.common.core.config.FileStorageProperties;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.security.domain.LoginUser;
import cn.swiftadmin.schedule.api.entity.ScheduleJob;
import cn.swiftadmin.schedule.executor.impl.BeanJobExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 不启动 Spring 容器的安全回归测试。
 */
class SecurityFixesTest {

    @AfterEach
    void resetIpTrust() {
        ServletUtils.setTrustForwardedHeaders(false);
        ServletUtils.clearIpCache();
    }

    @Test
    @DisplayName("默认不采信客户端伪造的转发头")
    void clientIpIgnoresSpoofedForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.1.1");
        request.addHeader("X-Forwarded-For", "8.8.8.8, 9.9.9.9");
        request.addHeader("X-Real-IP", "1.2.3.4");

        assertThat(ServletUtils.getClientIp(request)).isEqualTo("10.1.1.1");

        ServletUtils.setTrustForwardedHeaders(true);
        ServletUtils.clearIpCache();
        MockHttpServletRequest trusted = new MockHttpServletRequest();
        trusted.setRemoteAddr("10.1.1.1");
        trusted.addHeader("X-Forwarded-For", "8.8.8.8, 9.9.9.9");
        assertThat(ServletUtils.getClientIp(trusted)).isEqualTo("9.9.9.9");
    }

    @Test
    @DisplayName("上传目录不能跳出存储根，且能按相对路径读回")
    void localUploadRejectsTraversalAndRoundTrips() throws Exception {
        Path dir = Files.createTempDirectory("swiftadmin-upload");
        LocalFileStorageStrategy strategy = new LocalFileStorageStrategy();
        ReflectionTestUtils.setField(strategy, "basePath", dir.toString());
        ReflectionTestUtils.setField(strategy, "accessUrl", "/files");
        ReflectionTestUtils.setField(strategy, "maxFileSize", 10_000_000L);
        strategy.init();

        FileStorageProperties properties = new FileStorageProperties();
        FileUploadService uploadService = new FileUploadService(java.util.List.of(strategy), properties);
        uploadService.init();

        MockMultipartFile png = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3, 4});
        assertThatThrownBy(() -> uploadService.upload(png, "../../etc", "local"))
                .isInstanceOf(FileStorageException.class);
        assertThat(Files.list(dir).count()).isZero();

        MockMultipartFile html = new MockMultipartFile("file", "a.html", "text/html", "<script>".getBytes());
        assertThatThrownBy(() -> uploadService.upload(html, null, "local"))
                .isInstanceOf(FileStorageException.class);

        FileInfo saved = uploadService.upload(png, "docs", "local");
        assertThat(saved.getFileId()).doesNotContain("..");
        assertThat(saved.getPath()).isEqualTo(saved.getFileId());
        assertThat(strategy.getBytes(saved.getFileId())).containsExactly(1, 2, 3, 4);
        assertThat(Files.exists(dir.resolve(saved.getFileId()))).isTrue();

        assertThatThrownBy(() -> strategy.getBytes("../" + dir.getFileName()))
                .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> StoragePaths.sanitizeRelativeDir("/etc"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    @DisplayName("LoginUser 序列化不包含密码哈希")
    void loginUserJsonOmitsPassword() {
        LoginUser user = new LoginUser(1L, "admin", "$2a$secret-hash", 1);
        String json = new ObjectMapper().writeValueAsString(user);
        assertThat(json).doesNotContain("secret-hash");
        assertThat(json).doesNotContain("password");
    }

    @Test
    @DisplayName("定时任务不能反射调用 Spring 基础设施 Bean")
    void beanJobRejectsInfrastructureBeans() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean("sample")).thenReturn(new SampleJob());
        when(context.getBean("env")).thenReturn(new StandardEnvironment());
        BeanJobExecutor executor = new BeanJobExecutor(context, new ObjectMapper());

        ScheduleJob allowed = ScheduleJob.builder()
                .jobName("ok")
                .beanName("sample")
                .methodName("ping")
                .build();
        assertThat(executor.validate(allowed)).isTrue();
        assertThat(executor.execute(allowed)).isEqualTo("pong");

        ScheduleJob denied = ScheduleJob.builder()
                .jobName("bad")
                .beanName("env")
                .methodName("getProperty")
                .methodParams("[\"java.version\"]")
                .build();
        assertThat(executor.validate(denied)).isFalse();
    }

    /** 位于业务包内，允许被调度任务调用 */
    public static class SampleJob {
        public String ping() {
            return "pong";
        }
    }
}
