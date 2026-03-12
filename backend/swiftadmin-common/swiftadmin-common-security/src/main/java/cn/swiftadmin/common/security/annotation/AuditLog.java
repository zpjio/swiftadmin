package cn.swiftadmin.common.security.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * <p>标注在 Service 方法上，自动记录数据变更审计日志</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 模块名称
     */
    String module();

    /**
     * 操作类型
     */
    OperationType operation();

    /**
     * 实体类（用于获取表名和ID）
     */
    Class<?> entityClass();

    /**
     * 记录名称表达式（SpEL）
     * <p>例如："{#entity.name}" 或 "{#dto.name}"</p>
     */
    String recordName() default "";

    /**
     * 是否记录变更前数据
     */
    boolean recordBeforeData() default true;

    /**
     * 是否记录变更后数据
     */
    boolean recordAfterData() default true;

    /**
     * 忽略的字段名
     */
    String[] ignoreFields() default {"createTime", "updateTime", "deleted"};

    /**
     * 操作类型枚举
     */
    enum OperationType {
        /** 新增 */
        INSERT,
        /** 修改 */
        UPDATE,
        /** 删除 */
        DELETE
    }

}