package cn.swiftadmin.common.mybatis.aspect;

import cn.swiftadmin.common.mybatis.annotation.DataScope;
import cn.swiftadmin.common.mybatis.datascope.DataScopeInfo;
import cn.swiftadmin.common.mybatis.datascope.DataScopeManager;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据权限切面
 * <p>处理 @DataScope 注解，将数据权限信息存入 ThreadLocal</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(cn.swiftadmin.common.mybatis.annotation.DataScope)")
    public void doBefore(JoinPoint point) {
        // 获取注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        DataScope dataScope = method.getAnnotation(DataScope.class);

        // 保存注解信息到线程本地变量（用于 DataScopeHandler）
        DataScopeContext context = new DataScopeContext(dataScope);
        CONTEXT.set(context);
    }

    @After("@annotation(cn.swiftadmin.common.mybatis.annotation.DataScope)")
    public void doAfter() {
        // 清除线程本地变量
        CONTEXT.remove();
        DataScopeManager.clearDataScopeInfo();
    }

    /**
     * 数据权限上下文（用于 DataScopeHandler）
     */
    private static final ThreadLocal<DataScopeContext> CONTEXT = new ThreadLocal<>();

    /**
     * 获取当前数据权限上下文
     */
    public static DataScopeContext getContext() {
        return CONTEXT.get();
    }

    /**
     * 数据权限上下文
     */
    public static class DataScopeContext {
        private final DataScope dataScope;

        public DataScopeContext(DataScope dataScope) {
            this.dataScope = dataScope;
        }

        public DataScope getDataScope() {
            return dataScope;
        }
    }

}