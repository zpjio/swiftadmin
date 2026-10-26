package cn.swiftadmin.config;

import java.util.Map;

import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * MyBatis Mapper BeanDefinition 的 GraalVM native/AOT 适配。
 *
 * <p>问题根因:{@code @MapperScan} 扫描出的 {@link MapperFactoryBean} 定义里,
 * mapper 接口类以 String 形式挂在构造参数上,且 target 类型不带泛型。Spring AOT
 * 为此生成的实例供应商无法从定义里解析出 Class 参数,native 运行时按 autowire
 * 方式解析 {@code Class<?>} 抛出 "No qualifying bean of type
 * 'java.lang.Class<?>'"。
 *
 * <p>处理思路移植自 mybatis-spring-native 官方实现
 * {@code org.mybatis.spring.nativex.MyBatisMapperFactoryBeanPostProcessor}
 * (其绑定的 spring-native 专属 API 在 Boot AOT 下已不存在),效果一致:
 * 构造参数以真实 Class 值物化 + targetType 补建立健全的
 * {@code MapperFactoryBean<具体Mapper>} 泛型。普通 JVM 运行不受影响
 * (泛型信息本来就是可推断),native 运行时则依赖该改写生成正确可用的
 * 实例参数来源。
 *
 * @author zhangpengju
 * @since 2026-10-25
 */
@Component
public class MyBatisMapperDefinitionPostProcessor implements BeanFactoryPostProcessor {

    private static final String MAPPER_FACTORY_BEAN = MapperFactoryBean.class.getName();

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
            return;
        }
        // getBeanDefinitionNames 返回快照数组,循环中移除并重注册定义安全
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
            if (mapperInterface == null) {
                continue;
            }
            registry.removeBeanDefinition(beanName);
            registry.registerBeanDefinition(beanName, createTypedDefinition(abstractDefinition, mapperInterface));
        }
    }

    private boolean isMapperFactoryBeanDefinition(AbstractBeanDefinition abstractDefinition) {
        String className = abstractDefinition.hasBeanClass()
                ? abstractDefinition.getBeanClass().getName()
                : abstractDefinition.getBeanClassName();
        return MAPPER_FACTORY_BEAN.equals(className);
    }

    /**
     * 解析 mapper 接口类型,优先取定义属性上的 mapperInterface。
     */
    private Class<?> resolveMapperInterface(ClassLoader classLoader, AbstractBeanDefinition abstractDefinition) {
        Object mapperInterface = abstractDefinition.getPropertyValues().get("mapperInterface");
        if (mapperInterface instanceof Class<?> type) {
            return type;
        }
        if (mapperInterface instanceof String className) {
            return resolveClass(classLoader, className);
        }
        // 兜底:旧版 mybatis-spring 只在构造参数里携带类名 String
        ConstructorArgumentValues values = abstractDefinition.getConstructorArgumentValues();
        for (ConstructorArgumentValues.ValueHolder holder : values.getGenericArgumentValues()) {
            if (holder.getValue() instanceof String className) {
                return resolveClass(classLoader, className);
            }
        }
        for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry
                : values.getIndexedArgumentValues().entrySet()) {
            if (entry.getValue().getValue() instanceof String className) {
                return resolveClass(classLoader, className);
            }
        }
        return null;
    }

    private Class<?> resolveClass(ClassLoader classLoader, String className) {
        try {
            return ClassUtils.forName(className, classLoader);
        } catch (ClassNotFoundException | LinkageError ex) {
            // 类不可达时保持原定义,交由原运行时机制处理
            return null;
        }
    }

    private AbstractBeanDefinition createTypedDefinition(AbstractBeanDefinition beanDefinition, Class<?> mapperInterface) {
        RootBeanDefinition typed = new RootBeanDefinition(MapperFactoryBean.class);
        typed.setTargetType(ResolvableType.forClassWithGenerics(MapperFactoryBean.class, mapperInterface));
        typed.setScope(beanDefinition.getScope());
        typed.setLazyInit(beanDefinition.isLazyInit());
        typed.setAutowireCandidate(beanDefinition.isAutowireCandidate());
        // 注入一律走下方显式声明,不再依赖 by-type 自动装配
        // (by-type 值不会进入 AOT 生成的定义,native 运行期无法解析)
        typed.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
        typed.setPrimary(beanDefinition.isPrimary());
        typed.setRole(beanDefinition.getRole());
        typed.setSynthetic(beanDefinition.isSynthetic());
        typed.setDependsOn(beanDefinition.getDependsOn());
        if (beanDefinition.getInitMethodNames() != null) {
            typed.setInitMethodNames(beanDefinition.getInitMethodNames());
        }
        if (beanDefinition.getDestroyMethodNames() != null) {
            typed.setDestroyMethodNames(beanDefinition.getDestroyMethodNames());
        }
        typed.setPropertyValues(new MutablePropertyValues(beanDefinition.getPropertyValues()));
        declareSessionDependencies(typed);
        rewriteConstructorArguments(typed, beanDefinition, mapperInterface);
        return typed;
    }

    /**
     * 显式声明原 mybatis-spring 扫描器靠 autowire-by-type 在运行期解析的
     * 两个依赖:AOT 代码生成只编码定义里的属性值,by-type 自动装配在
     * native 运行期不适用,故转为按 bean 名引用
     * (flex 自动装配按方法名生成的 bean 名,见 MybatisFlexAutoConfiguration)。
     */
    private void declareSessionDependencies(RootBeanDefinition typed) {
        MutablePropertyValues propertyValues = typed.getPropertyValues();
        if (!propertyValues.contains("sqlSessionFactory")) {
            propertyValues.add("sqlSessionFactory", new RuntimeBeanReference("sqlSessionFactory"));
        }
        if (!propertyValues.contains("sqlSessionTemplate")) {
            propertyValues.add("sqlSessionTemplate", new RuntimeBeanReference("sqlSessionTemplate"));
        }
    }

    /**
     * 构造参数物化:类名 String 换成 mapper 接口 Class,其余值原样保留。
     */
    private void rewriteConstructorArguments(RootBeanDefinition typed, AbstractBeanDefinition beanDefinition,
            Class<?> mapperInterface) {
        ConstructorArgumentValues oldValues = beanDefinition.getConstructorArgumentValues();
        ConstructorArgumentValues newValues = typed.getConstructorArgumentValues();
        for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry
                : oldValues.getIndexedArgumentValues().entrySet()) {
            newValues.addIndexedArgumentValue(entry.getKey(), materialize(entry.getValue(), mapperInterface));
        }
        for (ConstructorArgumentValues.ValueHolder holder : oldValues.getGenericArgumentValues()) {
            newValues.addGenericArgumentValue(materialize(holder, mapperInterface));
        }
    }

    private ConstructorArgumentValues.ValueHolder materialize(
            ConstructorArgumentValues.ValueHolder holder, Class<?> mapperInterface) {
        Object value = holder.getValue();
        if (value instanceof String className && mapperInterface.getName().equals(className)) {
            value = mapperInterface;
        }
        ConstructorArgumentValues.ValueHolder materialized = new ConstructorArgumentValues.ValueHolder(value);
        if (holder.getType() != null) {
            materialized.setType(holder.getType());
        }
        if (holder.getName() != null) {
            materialized.setName(holder.getName());
        }
        return materialized;
    }
}
