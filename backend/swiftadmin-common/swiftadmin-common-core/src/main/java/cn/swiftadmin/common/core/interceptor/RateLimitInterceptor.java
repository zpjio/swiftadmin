package cn.swiftadmin.common.core.interceptor;

import cn.swiftadmin.common.core.annotation.RateLimit;
import cn.swiftadmin.common.core.util.ServletUtils;
import cn.swiftadmin.common.core.util.ErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.List;

/**
 * 限流拦截器
 * <p>基于 Redis + Lua 实现分布式限流，保证原子性操作</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final ErrorResponseWriter errorResponseWriter;
    private final DefaultRedisScript<Long> rateLimitScript;

    /**
     * Lua 脚本：原子性实现限流逻辑
     * <p>INCR 计数 + EXPIRE 设置过期时间，超出限制返回 -1</p>
     */
    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])

            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end

            if current > limit then
                return -1
            end

            return current
            """;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate, 
                                 ErrorResponseWriter errorResponseWriter,
                                 ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.errorResponseWriter = errorResponseWriter;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptText(LUA_SCRIPT);
        this.rateLimitScript.setResultType(Long.class);
    }

    /**
     * 请求前处理：检查是否触发限流
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String key = generateKey(request, rateLimit);
        long timeWindow = rateLimit.timeUnit().toSeconds(rateLimit.timeWindow());
        int maxRequests = rateLimit.maxRequests();

        List<String> keys = Collections.singletonList(key);
        Long result = redisTemplate.execute(rateLimitScript, keys, String.valueOf(maxRequests), String.valueOf(timeWindow));

        if (result != null && result == -1) {
            log.warn("限流触发：key={}, max={}", key, maxRequests);
            errorResponseWriter.writeError(response, 429, rateLimit.message());
            return false;
        }

        return true;
    }

    /**
     * 生成限流 Key
     * <p>支持自定义 Key，默认使用 IP + 方法 + 路径</p>
     *
     * @param request HTTP 请求
     * @param rateLimit 限流注解
     * @return 限流 Key
     */
    private String generateKey(HttpServletRequest request, RateLimit rateLimit) {
        String prefix = "rate_limit:";

        if (!rateLimit.key().isEmpty()) {
            return prefix + rateLimit.key();
        }

        String ip = ServletUtils.getClientIp(request);
        String method = request.getMethod();
        String path = request.getRequestURI();

        return prefix + ip + ":" + method + ":" + path;
    }

}
