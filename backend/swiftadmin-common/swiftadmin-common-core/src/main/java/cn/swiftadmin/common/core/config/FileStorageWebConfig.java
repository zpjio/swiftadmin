package cn.swiftadmin.common.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 文件存储 Web 配置
 * <p>配置本地文件的静态资源访问</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileStorageWebConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置本地文件访问路径
        String basePath = fileStorageProperties.getLocal().getBasePath();
        String accessUrl = fileStorageProperties.getLocal().getAccessUrl();
        
        log.info("配置本地文件访问：{} -> {}", accessUrl, basePath);
        
        registry.addResourceHandler(accessUrl + "/**")
                .addResourceLocations("file:" + basePath + "/");
    }
}
