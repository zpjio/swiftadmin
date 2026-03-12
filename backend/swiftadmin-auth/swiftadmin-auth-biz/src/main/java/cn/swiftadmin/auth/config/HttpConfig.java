package cn.swiftadmin.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置
 * <p>提供 RestTemplate Bean，用于第三方 API 调用</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Configuration
public class HttpConfig {

    /**
     * 配置 RestTemplate
     * <p>设置连接超时和读取超时</p>
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 连接超时 5 秒
        factory.setReadTimeout(10000);    // 读取超时 10 秒
        return new RestTemplate(factory);
    }

}
