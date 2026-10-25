package cn.swiftadmin.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 序列化配置（Jackson 3 / tools.jackson）
 * <p>配置 Long 类型序列化为字符串，避免前端（JS Number &gt; 2^53）精度丢失问题</p>
 *
 * <p>自 Boot 4 起 HTTP 消息转换器统一走 Jackson 3；项目内部消费路径
 * 也已全量迁移 {@code tools.jackson}。jjwt-jackson 因生态仍绑定
 * Jackson 2 databind（{@code com.fasterxml} 包名，与 Jackson 3 隔离共存），
 * 由其自带依赖满足，不受本配置影响。</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Configuration
public class JacksonConfig {

    /** Jackson 3：模块注册 Long → String（Jackson 3 Builder 无 serializerByType API） */
    @Bean
    public JsonMapperBuilderCustomizer longToStringJsonMapperCustomizer() {
        SimpleModule longToString = new SimpleModule("long-to-string")
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(Long.TYPE, ToStringSerializer.instance);
        return builder -> builder.addModule(longToString);
    }
}
