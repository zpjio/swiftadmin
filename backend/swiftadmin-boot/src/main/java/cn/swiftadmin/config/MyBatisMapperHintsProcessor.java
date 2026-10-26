package cn.swiftadmin.config;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.swiftadmin.auth.api.entity.SocialBindEncryptSetListener;
import cn.swiftadmin.common.mybatis.encrypt.AesGcmTypeHandler;
import com.mybatisflex.core.handler.CompositeEnumTypeHandler;
import com.mybatisflex.core.provider.EntitySqlProvider;
import com.mybatisflex.core.provider.RowSqlProvider;
import com.mybatisflex.core.row.RowMapper;
import com.mybatisflex.spring.boot.SpringBootVFS;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.javassist.util.proxy.ProxyFactory;
import org.apache.ibatis.javassist.util.proxy.RuntimeSupport;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 为 {@code @MapperScan} 扫描出的全部 MyBatis Mapper 接口注册 GraalVM native
 * 运行时提示:接口全量反射、JDK 动态代理、mapper XML 资源,以及各方法
 * 返回/参数类型(实体与 SqlProvider)的反射提示。
 *
 * <p>移植自 mybatis-spring-native 官方实现
 * {@code org.mybatis.spring.nativex.MyBatisMapperNativeConfigurationProcessor},
 * 原 API(spring-native 的 NativeConfigurationRegistry)已被 Boot AOT 取代,
 * 底层逻辑保持一致。经 {@code META-INF/spring/aot.factories} 加载,只在
 * native 构建的 AOT 处理阶段生效。
 *
 * @author zhangpengju
 * @since 2026-10-25
 */
public class MyBatisMapperHintsProcessor implements BeanFactoryInitializationAotProcessor {

    private static final String MAPPER_FACTORY_BEAN = MapperFactoryBean.class.getName();

    private static final String PROXY_CONFIG_PATH =
            "META-INF/native-image/cn.swiftadmin/swiftadmin-boot/proxy-config.json";

    private static final MemberCategory[] ALL_MEMBER_CATEGORIES = MemberCategory.values();

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(
            ConfigurableListableBeanFactory beanFactory) {
        List<Class<?>> mapperInterfaces = new ArrayList<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (!(beanDefinition instanceof AbstractBeanDefinition abstractDefinition)) {
                continue;
            }
            if (!isMapperFactoryBeanDefinition(abstractDefinition)) {
                continue;
            }
            Class<?> mapperInterface =
                    resolveMapperInterface(beanFactory.getBeanClassLoader(), abstractDefinition);
            if (mapperInterface != null) {
                mapperInterfaces.add(mapperInterface);
            }
        }
        if (mapperInterfaces.isEmpty()) {
            return null;
        }
        return (context, code) -> {
            RuntimeHints hints = context.getRuntimeHints();
            registerMyBatisCoreHints(hints);
            for (Class<?> mapperInterface : mapperInterfaces) {
                registerMapperHints(hints, mapperInterface);
            }
            // application.yml: mybatis-flex.mapper-locations: classpath*:mapper/**/*.xml
            hints.resources().registerPattern("mapper/**/*.xml");
            registerCglibSyntheticClasses(beanFactory, context);
            writeProxyConfig(context, mapperInterfaces);
        };
    }

    /**
     * 为应用自身的 AOT 生成 CGLIB 合成类补全反射注册。
     *
     * <p>Framework 7 对 CGLIB 增强类只默认注册构造器访问,但 Spring 在
     * native 运行期还会通过 {@code Enhancer.wrapCachedClass} 反射写
     * {@code CGLIB$FACTORY_DATA} 静态字段(如 {@code @Lazy} 注入、AOP
     * 代理初始化),缺字段注册会抛 MissingReflectionRegistrationError。
     * 按 bean 定义逐一探测同名合成类 {@code <fqcn>$$SpringCGLIB$$0} 是否
     * 存在,存在才注册,避免悬空 hint。</p>
     */
    private void registerCglibSyntheticClasses(ConfigurableListableBeanFactory beanFactory, GenerationContext context) {
        ClassLoader classLoader = beanFactory.getBeanClassLoader();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (!(beanDefinition instanceof AbstractBeanDefinition abstractDefinition)) {
                continue;
            }
            String className = abstractDefinition.hasBeanClass()
                    ? abstractDefinition.getBeanClass().getName()
                    : abstractDefinition.getBeanClassName();
            if (className == null || !className.startsWith("cn.swiftadmin.")) {
                continue;
            }
            registerIfPresent(className + "$$SpringCGLIB$$0", classLoader, context.getRuntimeHints());
        }
    }

    private void registerIfPresent(String className, ClassLoader classLoader, RuntimeHints hints) {
        try {
            Class<?> type = ClassUtils.forName(className, classLoader);
            hints.reflection().registerType(type, ALL_MEMBER_CATEGORIES);
        } catch (ClassNotFoundException | LinkageError ex) {
            // 该 bean 没有被代理增强,合成类不存在,跳过
        }
    }

    /**
     * 生成经典通道的 proxy-config.json。
     *
     * <p>Framework 7 的 RuntimeHintsWriter 只持久化 reflection/jni/resources
     * 三类提示,{@code proxies()} 里的 JDK 代理注册不会进入
     * reachability-metadata.json,但 GraalVM 仍支持读取
     * META-INF/native-image/&lt;group-id&gt;/&lt;artifact-id&gt;/proxy-config.json。
     * 把 mapper 代理所需接口统一在此写出,新增 mapper 无需手动维护文件。</p>
     */
    private void writeProxyConfig(GenerationContext context, List<Class<?>> mapperInterfaces) {
        StringBuilder json = new StringBuilder("[\n");
        for (Class<?> mapperInterface : mapperInterfaces) {
            json.append("  {\"interfaces\": [\"").append(mapperInterface.getName()).append("\"]},\n");
        }
        json.append("  {\"interfaces\": [\"com.mybatisflex.core.row.RowMapper\"]},\n");
        json.append("  {\"interfaces\": [\"jakarta.servlet.http.HttpServletRequest\"]}\n");
        json.append("]\n");
        context.getGeneratedFiles().addResourceFile(PROXY_CONFIG_PATH, json.toString());
    }

    /**
     * mybatis 核心反射注册(移植 mybatis-spring-native 的
     * MyBatisCoreNativeConfiguration):AOT 生成的实例供应商在运行期反射
     * 调用 {@link MapperFactoryBean} 构造器;语言驱动、javassist 代理、
     * 日志实现与二级缓存实现按官方清单注册;mapper XML 解析需要
     * DTD/XSD 资源。
     */
    private void registerMyBatisCoreHints(RuntimeHints hints) {
        hints.reflection().registerType(MapperFactoryBean.class, ALL_MEMBER_CATEGORIES);
        hints.reflection().registerType(SqlSessionFactoryBean.class, ALL_MEMBER_CATEGORIES);
        hints.reflection().registerType(SqlSessionFactory.class, ALL_MEMBER_CATEGORIES);
        for (Class<?> type : new Class<?>[] {
                XMLLanguageDriver.class, RawLanguageDriver.class,
                RuntimeSupport.class, ProxyFactory.class,
                Log.class, Slf4jImpl.class, JakartaCommonsLoggingImpl.class,
                Jdk14LoggingImpl.class, Log4j2Impl.class, StdOutImpl.class,
                NoLoggingImpl.class, PerpetualCache.class, FifoCache.class,
                LruCache.class, SoftCache.class, WeakCache.class}) {
            hints.reflection().registerType(type, ALL_MEMBER_CATEGORIES);
        }
        hints.resources().registerPattern("org/apache/ibatis/builder/xml/*.dtd");
        hints.resources().registerPattern("org/apache/ibatis/builder/xml/*.xsd");
        // mybatis-spring XML 配置 schema(官方 MyBatisSpringNativeConfiguration 清单)
        hints.resources().registerPattern("org/mybatis/spring/config/*.xsd");
        // flex 自动装配把 VFS 设为 SpringBootVFS,mybatis VFSHolder 在 <clinit>
        // 阶段反射实例化它;缺注册会在 TableInfoFactory.init 的包扫描里 NPE。
        // 图片 classpath 无目录,扫描必定返回空集,反射实例化正常即无 NPE,
        // TableInfo 改由查询期 ofMapperClass 按泛型惰性构建。
        hints.reflection().registerType(SpringBootVFS.class, ALL_MEMBER_CATEGORIES);
        // flex 内建默认 mapper(RowMapper):flex 启动时 configuration.addMapper
        // 需要 JDK 代理与接口反射
        hints.reflection().registerType(RowMapper.class, ALL_MEMBER_CATEGORIES);
        hints.proxies().registerJdkProxy(RowMapper.class);
        // flex 内建 SqlProvider:BaseMapper/RowMapper 各 CRUD 语句由 mybatis
        // ProviderSqlSource 反射查到方法并调用(ProviderUtil 为包私有类,
        // 由本包内部直调,不在反射面里)
        hints.reflection().registerType(EntitySqlProvider.class, ALL_MEMBER_CATEGORIES);
        hints.reflection().registerType(RowSqlProvider.class, ALL_MEMBER_CATEGORIES);
        // flex 枚举 type handler:字段为枚举时 mybatis TypeHandlerRegistry
        // 反射实例化它
        hints.reflection().registerType(CompositeEnumTypeHandler.class, ALL_MEMBER_CATEGORIES);
        // 项目自定义的 flex 生命周期类:@Table onSet 监听器与 @Column
        // typeHandler 均由 flex/mybatis 反射实例化
        hints.reflection().registerType(SocialBindEncryptSetListener.class, ALL_MEMBER_CATEGORIES);
        hints.reflection().registerType(AesGcmTypeHandler.class, ALL_MEMBER_CATEGORIES);
    }

    private void registerMapperHints(RuntimeHints hints, Class<?> mapperInterface) {
        hints.reflection().registerType(mapperInterface, ALL_MEMBER_CATEGORIES);
        hints.proxies().registerJdkProxy(mapperInterface);
        hints.resources().registerPattern(
                mapperInterface.getName().replace('.', '/').concat(".xml"));
        for (Method method : ReflectionUtils.getAllDeclaredMethods(mapperInterface)) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            ReflectionUtils.makeAccessible(method);
            registerSqlProviderTypes(hints, method,
                    SelectProvider.class, SelectProvider::value, SelectProvider::type);
            registerSqlProviderTypes(hints, method,
                    InsertProvider.class, InsertProvider::value, InsertProvider::type);
            registerSqlProviderTypes(hints, method,
                    UpdateProvider.class, UpdateProvider::value, UpdateProvider::type);
            registerSqlProviderTypes(hints, method,
                    DeleteProvider.class, DeleteProvider::value, DeleteProvider::type);
            registerReflectionTypeIfNecessary(hints, resolveReturnClass(mapperInterface, method));
            resolveParameterClasses(mapperInterface, method)
                    .forEach(type -> registerReflectionTypeIfNecessary(hints, type));
        }
    }

    @SafeVarargs
    private final <T extends java.lang.annotation.Annotation> void registerSqlProviderTypes(
            RuntimeHints hints, Method method, Class<T> annotationType,
            java.util.function.Function<T, Class<?>>... providerTypeResolvers) {
        for (T annotation : method.getAnnotationsByType(annotationType)) {
            for (java.util.function.Function<T, Class<?>> providerTypeResolver : providerTypeResolvers) {
                registerReflectionTypeIfNecessary(hints, providerTypeResolver.apply(annotation));
            }
        }
    }

    private void registerReflectionTypeIfNecessary(RuntimeHints hints, Class<?> type) {
        if (type != null && !type.isPrimitive() && !type.getName().startsWith("java")) {
            hints.reflection().registerType(type, ALL_MEMBER_CATEGORIES);
        }
    }

    private boolean isMapperFactoryBeanDefinition(AbstractBeanDefinition abstractDefinition) {
        String className = abstractDefinition.hasBeanClass()
                ? abstractDefinition.getBeanClass().getName()
                : abstractDefinition.getBeanClassName();
        return MAPPER_FACTORY_BEAN.equals(className);
    }

    private Class<?> resolveMapperInterface(ClassLoader classLoader,
            AbstractBeanDefinition abstractDefinition) {
        Object mapperInterface = abstractDefinition.getPropertyValues().get("mapperInterface");
        if (mapperInterface instanceof Class<?> type) {
            return type;
        }
        for (ConstructorArgumentValues.ValueHolder holder
                : abstractDefinition.getConstructorArgumentValues().getGenericArgumentValues()) {
            if (holder.getValue() instanceof String className) {
                try {
                    return ClassUtils.forName(className, classLoader);
                } catch (ClassNotFoundException | LinkageError ex) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 解析方法的实际返回类型(处理泛型与数组),逻辑与 mybatis-spring-native
     * 的 MyBatisMapperTypeUtils 一致。
     */
    private Class<?> resolveReturnClass(Class<?> mapperInterface, Method method) {
        Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
        return typeToClass(resolvedReturnType, method.getReturnType());
    }

    private Set<Class<?>> resolveParameterClasses(Class<?> mapperInterface, Method method) {
        return Stream.of(TypeParameterResolver.resolveParamTypes(method, mapperInterface))
                .map(type -> typeToClass(type, type instanceof Class<?> ? (Class<?>) type : Object.class))
                .collect(Collectors.toSet());
    }

    private Class<?> typeToClass(Type src, Class<?> fallback) {
        Class<?> result = null;
        if (src instanceof Class<?> type) {
            result = type.isArray() ? type.getComponentType() : type;
        } else if (src instanceof ParameterizedType parameterizedType) {
            boolean mapWithUpperBound = parameterizedType.getRawType() instanceof Class<?> rawType
                    && java.util.Map.class.isAssignableFrom(rawType)
                    && parameterizedType.getActualTypeArguments().length > 1;
            int index = mapWithUpperBound ? 1 : 0;
            result = typeToClass(parameterizedType.getActualTypeArguments()[index], fallback);
        }
        return result != null ? result : fallback;
    }
}
