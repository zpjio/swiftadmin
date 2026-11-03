package cn.swiftadmin.schedule.executor.impl;

import cn.swiftadmin.schedule.api.entity.ScheduleJob;
import cn.swiftadmin.schedule.api.enums.JobType;
import cn.swiftadmin.schedule.executor.JobExecutor;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Bean 方式任务执行器
 * <p>通过 Spring 容器获取 Bean 并调用指定方法</p>
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BeanJobExecutor implements JobExecutor {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    /** 只允许调用本项目的 Bean，避免调度任务反射到数据源、环境等基础设施 */
    private static final String ALLOWED_PACKAGE = "cn.swiftadmin.";

    @Override
    public JobType getType() {
        return JobType.BEAN;
    }

    @Override
    public Object execute(ScheduleJob job) throws Exception {
        String beanName = job.getBeanName();
        String methodName = job.getMethodName();
        String methodParams = job.getMethodParams();

        // 从 Spring 容器获取 Bean
        Object bean = applicationContext.getBean(beanName);
        if (bean == null) {
            throw new IllegalArgumentException("Bean not found: " + beanName);
        }

        // 获取方法（仅本项目类型上声明的 public 方法）
        Method method = findMethod(bean, methodName);
        if (method == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }

        // 解析参数
        Object[] args = parseArguments(method, methodParams);

        // 执行方法
        return method.invoke(bean, args);
    }

    @Override
    public boolean validate(ScheduleJob job) {
        if (!StringUtils.hasText(job.getBeanName())) {
            log.error("Bean name is empty for job: {}", job.getJobName());
            return false;
        }
        if (!StringUtils.hasText(job.getMethodName())) {
            log.error("Method name is empty for job: {}", job.getJobName());
            return false;
        }

        try {
            Object bean = applicationContext.getBean(job.getBeanName());
            if (bean == null) {
                log.error("Bean not found: {} for job: {}", job.getBeanName(), job.getJobName());
                return false;
            }

            Method method = findMethod(bean, job.getMethodName());
            if (method == null) {
                log.error("Method not found: {} for job: {}", job.getMethodName(), job.getJobName());
                return false;
            }

            // 验证参数解析
            parseArguments(method, job.getMethodParams());
            return true;
        } catch (Exception e) {
            log.error("Job validation failed: {}", job.getJobName(), e);
            return false;
        }
    }

    /**
     * 查找本项目类型上声明的 public 方法。
     * <p>不接受 {@code java.*}、Spring 基础设施或 JDK 代理上继承来的方法</p>
     *
     * @param bean Spring Bean
     * @param methodName 方法名
     * @return 方法，不允许时返回 null
     */
    private Method findMethod(Object bean, String methodName) {
        Class<?> clazz = AopUtils.getTargetClass(bean);
        if (clazz == null || !clazz.getName().startsWith(ALLOWED_PACKAGE)) {
            log.error("拒绝调用非业务 Bean：{}", clazz == null ? "null" : clazz.getName());
            return null;
        }

        Class<?> current = clazz;
        while (current != null && current.getName().startsWith(ALLOWED_PACKAGE)) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isBridge() || !Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                if (method.getName().equals(methodName)) {
                    ReflectionUtils.makeAccessible(method);
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 解析方法参数
     *
     * @param method 方法
     * @param methodParams 参数字符串（JSON）
     * @return 参数数组
     */
    private Object[] parseArguments(Method method, String methodParams) {
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 如果没有参数
        if (parameterTypes.length == 0) {
            if (StringUtils.hasText(methodParams)) {
                log.warn("Method has no parameters but params provided");
            }
            return new Object[0];
        }

        // 如果有参数但未提供
        if (!StringUtils.hasText(methodParams)) {
            return new Object[parameterTypes.length];
        }

        try {
            // 尝试解析为 JSON 数组
            Object[] params = objectMapper.readValue(methodParams, Object[].class);

            if (params.length != parameterTypes.length) {
                log.warn("Parameter count mismatch: expected {}, got {}", parameterTypes.length, params.length);
            }

            return params;
        } catch (Exception e) {
            log.error("Failed to parse method params: {}", methodParams, e);
            throw new IllegalArgumentException("Invalid method parameters", e);
        }
    }
}
