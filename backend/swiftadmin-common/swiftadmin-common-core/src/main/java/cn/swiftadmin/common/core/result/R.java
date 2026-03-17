package cn.swiftadmin.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一响应类
 * <p>所有 REST 接口的响应格式，包含响应码、消息、数据、链路追踪 ID 和时间戳</p>
 *
 * @param <T> 响应数据类型
 * @author zhangpengju
 * @since 2026-03-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    /** 响应码：0 表示成功，其他表示失败 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 链路追踪 ID，用于日志追踪 */
    private String traceId;

    /** 响应时间戳 */
    private long timestamp;

    /** 错误详情（可选，失败时返回） */
    private ErrorDetail error;

    /**
     * 错误详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        /** 错误类型: PARAM | AUTH | BIZ | SYSTEM */
        private String type;

        /** 字段级错误（表单校验） */
        private List<FieldError> fieldErrors;

        /** 是否需要重试 */
        private Boolean retryable;

        /** 文档链接 */
        private String docUrl;

        /**
         * 添加字段错误
         */
        public ErrorDetail addFieldError(String field, String message) {
            if (this.fieldErrors == null) {
                this.fieldErrors = new ArrayList<>();
            }
            this.fieldErrors.add(new FieldError(field, message, null));
            return this;
        }

        /**
         * 添加字段错误（包含被拒绝的值）
         */
        public ErrorDetail addFieldError(String field, String message, Object rejectedValue) {
            if (this.fieldErrors == null) {
                this.fieldErrors = new ArrayList<>();
            }
            this.fieldErrors.add(new FieldError(field, message, rejectedValue));
            return this;
        }
    }

    /**
     * 字段级错误
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        /** 字段名 */
        private String field;

        /** 错误消息 */
        private String message;

        /** 被拒绝的值 */
        private Object rejectedValue;
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> R<T> ok(T data) {
        return new R<>(0, "操作成功", data, getTraceId(), System.currentTimeMillis(), null);
    }

    /**
     * 成功响应（带消息和数据）
     *
     * @param message 响应消息
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(0, message, data, getTraceId(), System.currentTimeMillis(), null);
    }

    /**
     * 成功响应（无数据）
     *
     * @return 成功响应
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * 失败响应（使用错误码枚举）
     *
     * @param errorCode 错误码枚举
     * @return 失败响应
     */
    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null, getTraceId(), System.currentTimeMillis(),
                createErrorDetail(errorCode));
    }

    /**
     * 失败响应（使用错误码枚举 + 自定义消息）
     *
     * @param errorCode 错误码枚举
     * @param message 自定义消息
     * @return 失败响应
     */
    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        return new R<>(errorCode.getCode(), message, null, getTraceId(), System.currentTimeMillis(),
                createErrorDetail(errorCode));
    }

    /**
     * 失败响应（自定义错误码和消息）
     *
     * @param code 错误码
     * @param message 错误消息
     * @return 失败响应
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null, getTraceId(), System.currentTimeMillis(), null);
    }

    /**
     * 失败响应（自定义错误码、消息和错误详情）
     *
     * @param code 错误码
     * @param message 错误消息
     * @param errorDetail 错误详情
     * @return 失败响应
     */
    public static <T> R<T> fail(int code, String message, ErrorDetail errorDetail) {
        return new R<>(code, message, null, getTraceId(), System.currentTimeMillis(), errorDetail);
    }

    /**
     * 失败响应（带字段错误）
     *
     * @param errorCode 错误码枚举
     * @param fieldErrors 字段错误列表
     * @return 失败响应
     */
    public static <T> R<T> fail(ErrorCode errorCode, List<FieldError> fieldErrors) {
        ErrorDetail errorDetail = createErrorDetail(errorCode);
        errorDetail.setFieldErrors(fieldErrors);
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null, getTraceId(), System.currentTimeMillis(),
                errorDetail);
    }

    /**
     * 失败响应（带错误详情）
     *
     * @param errorCode 错误码枚举
     * @param message 错误消息
     * @param errorDetail 错误详情
     * @return 失败响应
     */
    public static <T> R<T> fail(ErrorCode errorCode, String message, ErrorDetail errorDetail) {
        return new R<>(errorCode.getCode(), message, null, getTraceId(), System.currentTimeMillis(), errorDetail);
    }

    /**
     * 创建错误详情
     */
    private static ErrorDetail createErrorDetail(ErrorCode errorCode) {
        ErrorDetail detail = new ErrorDetail();
        detail.setType(getErrorTypeString(errorCode.getErrorType()));
        detail.setRetryable(errorCode.isSystemError());
        return detail;
    }

    /**
     * 获取错误类型字符串
     */
    private static String getErrorTypeString(int errorType) {
        return switch (errorType) {
            case 1 -> "PARAM";
            case 2 -> "AUTH";
            case 3 -> "BIZ";
            case 4 -> "SYSTEM";
            default -> "UNKNOWN";
        };
    }

    /**
     * 获取链路追踪 ID
     *
     * @return 追踪 ID，无则返回 "N/A"
     */
    private static String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "N/A";
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return code == 0;
    }

    /**
     * 是否失败
     */
    public boolean isFailed() {
        return code != 0;
    }
}