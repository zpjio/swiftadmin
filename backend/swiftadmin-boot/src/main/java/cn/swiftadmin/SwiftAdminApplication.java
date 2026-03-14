package cn.swiftadmin;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SwiftAdmin 启动类
 * <p>系统主入口，启用 Spring Boot 自动配置，扫描系统模块和认证模块的 Mapper</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@SpringBootApplication
@MapperScan({"cn.swiftadmin.system.mapper", "cn.swiftadmin.auth.mapper", "cn.swiftadmin.message.mapper"})
public class SwiftAdminApplication {

    private static final Logger logger = LoggerFactory.getLogger(SwiftAdminApplication.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        SpringApplication.run(SwiftAdminApplication.class, args);
        long startTimeCost = System.currentTimeMillis() - startTime;
        logger.info("系统启动完成，耗时：{} ms", startTimeCost);
        printDocUrls();
    }

    private static void printDocUrls() {
        final String baseUrl = "http://localhost:8080";

        logger.info("==================================================");
        logger.info("Swagger UI : {}/swagger-ui/index.html", baseUrl);
        logger.info("OpenAPI JSON: {}/v3/api-docs", baseUrl);
        logger.info("==================================================");
    }

}
