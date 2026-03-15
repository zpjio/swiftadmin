package cn.swiftadmin.common.mybatis.id;

import com.mybatisflex.core.keygen.IKeyGenerator;
import com.mybatisflex.core.keygen.KeyGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 雪花算法 ID 生成器自动配置
 * <p>
 * 提供可配置的雪花ID生成器，支持手动指定 workerId 和 datacenterId。
 * </p>
 *
 * <h3>配置项：</h3>
 * <pre>
 * swiftadmin:
 *   id-generator:
 *     enabled: true                  # 是否启用
 *     worker-id: 0                   # 工作机器ID (0-31)，不配置则自动生成
 *     datacenter-id: 0               # 数据中心ID (0-31)，不配置则自动生成
 * </pre>
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "swiftadmin.id-generator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SnowflakeIdAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdAutoConfiguration.class);

    /**
     * 工作机器ID (0-31)
     * <p>不配置时自动生成，适用于单机或容器环境</p>
     */
    @Value("${swiftadmin.id-generator.worker-id:-1}")
    private long workerId;

    /**
     * 数据中心ID (0-31)
     * <p>不配置时自动生成，适用于单机或容器环境</p>
     */
    @Value("${swiftadmin.id-generator.datacenter-id:-1}")
    private long datacenterId;

    /**
     * 初始化雪花ID生成器并注册到 MyBatis-Flex
     */
    @Bean
    public IKeyGenerator snowflakeIdGenerator() {
        ConfigurableSnowflakeIdGenerator generator = new ConfigurableSnowflakeIdGenerator(workerId, datacenterId);

        // 设置为默认实例，供其他代码使用
        ConfigurableSnowflakeIdGenerator.setDefaultInstance(generator);

        // 注册到 KeyGeneratorFactory，覆盖默认的 snowFlakeId
        KeyGeneratorFactory.register("snowFlakeId", generator);

        log.info("雪花ID生成器已注册: workerId={}, datacenterId={}",
                generator.getWorkerId(), generator.getDatacenterId());

        return generator;
    }
}