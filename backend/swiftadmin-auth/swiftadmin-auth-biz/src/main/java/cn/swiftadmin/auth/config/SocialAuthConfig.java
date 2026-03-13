package cn.swiftadmin.auth.config;

import cn.swiftadmin.auth.api.service.SocialBindService;
import cn.swiftadmin.auth.service.LoginEventService;
import cn.swiftadmin.auth.service.LoginSessionService;
import cn.swiftadmin.auth.strategy.SocialAuthStrategy;
import cn.swiftadmin.auth.strategy.social.SocialAuthHandler;
import cn.swiftadmin.system.api.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 社交登录配置类
 * <p>配置社交登录策略 Bean</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Configuration
@RequiredArgsConstructor
public class SocialAuthConfig {

    @Bean
    public SocialAuthStrategy socialAuthStrategy(
            SocialBindService socialBindService,
            UserService userService,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            LoginSessionService sessionService,
            LoginEventService eventService,
            HttpServletRequest request,
            List<SocialAuthHandler> handlers) {
        return new SocialAuthStrategy(
                socialBindService,
                userService,
                objectMapper,
                redisTemplate,
                sessionService,
                eventService,
                request,
                handlers);
    }
}