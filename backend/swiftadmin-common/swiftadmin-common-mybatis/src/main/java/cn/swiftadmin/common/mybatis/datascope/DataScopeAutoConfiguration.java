package cn.swiftadmin.common.mybatis.datascope;

import cn.swiftadmin.common.mybatis.aspect.DataScopeAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 数据权限自动配置
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class DataScopeAutoConfiguration {

    /**
     * 数据权限切面
     */
    @Bean
    @ConditionalOnMissingBean
    public DataScopeAspect dataScopeAspect() {
        log.debug("注册数据权限切面");
        return new DataScopeAspect();
    }

}