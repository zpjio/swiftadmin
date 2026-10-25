package cn.swiftadmin.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson 序列化配置
 * <p>配置 Long 类型序列化为字符串，避免前端（JS Number &gt; 2^53）精度丢失问题</p>
 *
 * <p>Spring Boot 4 起 HTTP 消息转换器走 Jackson 3（tools.jackson），
 * Jackson 2 的 customizer 只作用手动注入的 {@code com.fasterxml} ObjectMapper。
 * 两套必须各定一次；未来完成 Jackson 3 迁移后可拆除 jackson2 分支。</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Configuration
public class JacksonConfig {

    /** Jackson 3（Boot 4 的 HTTP 转换器链路）：模块注册 Long → String */
    @Bean
    public JsonMapperBuilderCustomizer longToStringJsonMapperCustomizer() {
        @SuppressWarnings("unchecked")
        SimpleModule longToString = new SimpleModule("long-to-string")
                .addSerializer(Long.class, tools.jackson.databind.ser.std.ToStringSerializer.instance)
                .addSerializer(Long.TYPE, tools.jackson.databind.ser.std.ToStringSerializer.instance);
        return builder -> builder.addModule(longToString);
    }

    /** Jackson 2（手动注入 {@code com.fasterxml} ObjectMapper 的内部消费路径） */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
