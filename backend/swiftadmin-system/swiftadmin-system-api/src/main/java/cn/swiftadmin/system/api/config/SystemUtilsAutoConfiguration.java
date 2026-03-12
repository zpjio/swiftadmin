package cn.swiftadmin.system.api.config;

import cn.swiftadmin.system.api.service.ConfigService;
import cn.swiftadmin.system.api.service.DictService;
import cn.swiftadmin.system.api.util.ConfigUtils;
import cn.swiftadmin.system.api.util.DictUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 系统工具类自动配置
 * <p>在 Spring 容器启动后，将服务实例注入到工具类中</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Configuration
public class SystemUtilsAutoConfiguration {

    private final DictService dictService;
    private final ConfigService configService;

    public SystemUtilsAutoConfiguration(DictService dictService, ConfigService configService) {
        this.dictService = dictService;
        this.configService = configService;
    }

    @PostConstruct
    public void init() {
        // 初始化字典工具类
        DictUtils.setDictService(dictService);
        // 初始化配置工具类
        ConfigUtils.setConfigService(configService);
    }

}