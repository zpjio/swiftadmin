package cn.swiftadmin.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 数据权限校验注解
 * <p>用于标注需要进行数据权限校验的 UPDATE/DELETE 方法</p>
 * <p>在方法执行前自动检查用户是否有权限操作目标数据</p>
 * <p>支持：单个用户ID、用户ID列表、DTO对象</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeValidate {

    /**
     * 数据权限校验模式
     */
    ValidateMode mode() default ValidateMode.THROW;

    /**
     * 校验失败时的错误消息
     */
    String message() default "无权限操作该数据";

    /**
     * 是否自动查询用户部门
     * <p>当只传入 userId 但没有 deptId 时，自动从用户服务查询该用户的部门</p>
     */
    boolean autoQueryDept() default true;

    /**
     * 批量校验模式
     * <p>当参数为 List<Long> 类型的用户ID列表时生效</p>
     */
    BatchMode batchMode() default BatchMode.ALL_OR_NONE;

    /**
     * 校验模式枚举
     */
    enum ValidateMode {
        /** 抛出异常（默认） */
        THROW,
        /** 返回 false，不抛异常 */
        RETURN_FALSE,
        /** 静默跳过校验（用于调试） */
        SKIP
    }

    /**
     * 批量校验模式
     */
    enum BatchMode {
        /** 全部通过才允许操作 */
        ALL_OR_NONE,
        /** 过滤无权限的数据，只操作有权限的数据 */
        FILTER,
        /** 只要有一个有权限就允许操作 */
        ANY
    }
}