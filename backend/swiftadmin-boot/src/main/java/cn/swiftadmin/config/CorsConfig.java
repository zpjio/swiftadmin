package cn.swiftadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置
 * <p>允许的源由 {@code swiftadmin.cors.allowed-origins} 配置（逗号分隔），生产环境务必使用白名单</p>
 * <p>配置为 {@code *} 时不允许携带凭据，避免任意站点携带用户凭据访问接口</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
@Configuration
public class CorsConfig {

    private final String allowedOrigins;

    public CorsConfig(
            @Value("${swiftadmin.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174}")
            String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * CORS 配置源
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        boolean wildcard = origins.contains("*");

        CorsConfiguration config = new CorsConfiguration();
        // 允许的源（白名单）
        config.setAllowedOriginPatterns(origins);
        // 允许的请求头
        config.setAllowedHeaders(List.of("*"));
        // 允许的请求方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 通配符场景下禁止携带凭据
        config.setAllowCredentials(!wildcard);
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);
        // 暴露的响应头
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
