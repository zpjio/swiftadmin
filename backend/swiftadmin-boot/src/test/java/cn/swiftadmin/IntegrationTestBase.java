package cn.swiftadmin;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 集成测试数据库/缓存支撑基类。
 *
 * <p>两种运行模式，互不干扰：</p>
 * <ul>
 *   <li><b>外部实例</b>（CI 与显式指定）：设置了 {@code DB_HOST} 环境变量时，不启动任何容器，
 *       连接信息直接来自环境变量（与 CI services / 本地 docker compose 保持一致）。</li>
 *   <li><b>临时容器</b>（全新机器零配置）：未设置 {@code DB_HOST} 时，自动用 Testcontainers
 *       拉起 mysql:8.0 + redis:7-alpine，属性经 {@link DynamicPropertySource} 注入，
 *       覆盖 application-dev.yml 的占位符默认值。</li>
 * </ul>
 *
 * <p>无 Docker 且未设置外部实例时，容器启动会直接失败并给出原因——这是预期边界。</p>
 */
abstract class IntegrationTestBase {

    /** 是否使用外部实例（设置了 DB_HOST 即视为指定）。 */
    private static final boolean USE_EXTERNAL = System.getenv("DB_HOST") != null;

    private static MySQLContainer<?> MYSQL;
    private static GenericContainer<?> REDIS;

    static {
        if (!USE_EXTERNAL) {
            MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("swiftadmin")
                    .withUsername("root")
                    .withPassword("root");
            MYSQL.start();
            REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
            REDIS.start();
        }
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        if (!USE_EXTERNAL) {
            registry.add("DB_HOST", MYSQL::getHost);
            registry.add("DB_PORT", () -> String.valueOf(MYSQL.getMappedPort(3306)));
            registry.add("DB_NAME", () -> MYSQL.getDatabaseName());
            registry.add("DB_USERNAME", MYSQL::getUsername);
            registry.add("DB_PASSWORD", MYSQL::getPassword);
            registry.add("REDIS_HOST", REDIS::getHost);
            registry.add("REDIS_PORT", () -> String.valueOf(REDIS.getMappedPort(6379)));
            registry.add("REDIS_PASSWORD", () -> "");
            registry.add("REDIS_DATABASE", () -> "7");
        }
    }
}
