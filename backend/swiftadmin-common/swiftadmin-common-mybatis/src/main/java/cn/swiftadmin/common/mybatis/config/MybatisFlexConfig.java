package cn.swiftadmin.common.mybatis.config;

import cn.swiftadmin.common.mybatis.encrypt.AesGcmTypeHandler;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 配置类
 * <p>配置逻辑删除、审计日志、加密等</p>
 * <p>脱敏使用 @ColumnMask 注解，内置支持 mobile、email 等类型</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Configuration
public class MybatisFlexConfig {

    private static final Logger log = LoggerFactory.getLogger(MybatisFlexConfig.class);

    /** Token 加密密钥 */
    @Value("${social.token-secret:swiftadmin-social-token-secret-key}")
    private String tokenSecret;

    /**
     * MyBatis-Flex 全局配置
     */
    @Bean
    public MyBatisFlexCustomizer myBatisFlexCustomizer() {
        return configuration -> {
            // 1. 禁用 Banner
            FlexGlobalConfig.getDefaultConfig().setPrintBanner(false);

            // 2. 设置 AES 加密密钥（用于 TypeHandler 解密）
            AesGcmTypeHandler.setSecretKey(tokenSecret);

            // 3. 开启审计日志
            AuditManager.setAuditEnable(true);
            AuditManager.setMessageCollector(auditMessage -> {
                // 输出 SQL 执行日志
                long elapsedTime = auditMessage.getElapsedTime();
                String fullSql = auditMessage.getFullSql();

                // 慢 SQL 告警阈值（毫秒）
                if (elapsedTime > 500) {
                    log.warn("慢SQL告警: {}ms, SQL: {}", elapsedTime, fullSql);
                } else if (log.isDebugEnabled()) {
                    log.debug("SQL 执行耗时: {}ms, SQL: {}", elapsedTime, fullSql);
                }
            });

            log.debug("MyBatis-Flex 配置完成");
        };
    }

}