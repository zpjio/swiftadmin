package cn.swiftadmin.common.mybatis.datascope;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据权限自动配置
 * <p>配置数据权限拦截器和工厂</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Slf4j
@Configuration
public class DataScopeAutoConfiguration {

    /**
     * 数据权限拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public DataScopeInterceptor dataScopeInterceptor() {
        log.info("注册数据权限拦截器");
        return new DataScopeInterceptor();
    }

}