package cn.swiftadmin.common.security.config;

import cn.swiftadmin.common.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 * <p>仅放行登录、注册、验证码、开放文档等必要接口，其余接口一律要求认证</p>
 * <p>使用 JWT 认证，禁用 CSRF 和表单登录</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 密码编码器（BCrypt）
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤链配置
     *
     * @param http HttpSecurity
     * @param jwtFilter JWT 认证过滤器
     * @return 安全过滤链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            .cors(cors -> cors.configure(http)) // 启用 CORS
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // ---------- 匿名可访问的认证接口 ----------
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/captcha", "/api/auth/captcha/required").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/sms/send").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/password/force-change").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/social/url/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/social/callback/**").permitAll()
                // 登录页需要读取登录方式配置
                .requestMatchers(HttpMethod.GET, "/api/auth/login-config/**").permitAll()

                // ---------- 健康检查与接口文档 ----------
                // 仅健康检查匿名可访问，其余 actuator 端点需要认证
                .requestMatchers("/health", "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(
                        "/doc.html",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/api-docs/**"
                ).permitAll()

                // ---------- 其余接口（含登出、改密、权限、业务接口）需认证 ----------
                .anyRequest().authenticated()
            )
            // 添加 JWT 过滤器
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // 禁用 HTTP Basic 和表单登录
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());

        return http.build();
    }

}
