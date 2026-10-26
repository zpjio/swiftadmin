package cn.swiftadmin.config;

import ch.qos.logback.classic.BasicConfigurator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.stereotype.Component;

/**
 * GraalVM native-image 运行时反射补注。
 *
 * <p>背景：主类的静态 logger 会在 {@code <clinit>} 阶段（早于 Spring 的
 * LoggingSystem 启动）触发 logback 自动装配；resources 中没有 {@code logback.xml} /
 * {@code logback-test.xml} 时，logback 会走反射回退实例化
 * {@link BasicConfigurator}。GraalVM 元数据仓库对 logback-classic 1.6.x
 * 尚未覆盖该条目，故在应用侧注册。</p>
 */
@Component
public class NativeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(
                BasicConfigurator.class,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
        );
        // 单例 bean(认证策略)构造注入 HttpServletRequest 时,Spring 走
        // resolveAutowiringValue 建立 JDK scoped 代理;native 下须先注册代理类
        hints.proxies().registerJdkProxy(HttpServletRequest.class);
    }
}
