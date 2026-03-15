package cn.swiftadmin.common.security.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>标注在 Controller 方法上，自动记录操作日志</p>
 * <p>支持记录数据变更（审计功能）</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作标题
     */
    String title() default "";

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 业务类型
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 操作类别
     */
    OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * 是否保存请求参数
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应参数
     */
    boolean isSaveResponseData() default true;

    /**
     * 排除指定的请求参数
     */
    String[] excludeParamNames() default {};

    // ==================== 审计功能属性 ====================

    /**
     * 实体类（用于获取表名）
     */
    Class<?> entityClass() default void.class;

    /**
     * 是否记录变更前数据
     */
    boolean isSaveBeforeData() default false;

    /**
     * 是否记录变更后数据
     */
    boolean isSaveAfterData() default false;

    /**
     * 忽略的字段名
     */
    String[] ignoreFields() default {"createTime", "updateTime", "deleted"};

    /**
     * 业务类型枚举
     */
    enum BusinessType {
        /** 其它 */
        OTHER(0),
        /** 新增 */
        INSERT(1),
        /** 修改 */
        UPDATE(2),
        /** 删除 */
        DELETE(3),
        /** 授权 */
        GRANT(4),
        /** 导出 */
        EXPORT(5),
        /** 导入 */
        IMPORT(6),
        /** 强退 */
        FORCE(7),
        /** 生成代码 */
        GENCODE(8),
        /** 清空数据 */
        CLEAN(9),
        /** 创建 */
        CREATE(10),
        /** 刷新 */
        REFRESH(11);

        private final int code;

        BusinessType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * 操作类别枚举
     */
    enum OperatorType {
        /** 其它 */
        OTHER(0),
        /** 后台用户 */
        MANAGE(1),
        /** 手机端用户 */
        MOBILE(2);

        private final int code;

        OperatorType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

}