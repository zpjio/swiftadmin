package cn.swiftadmin.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

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

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> R<T> ok(T data) {
        return new R<>(0, "操作成功", data, getTraceId(), System.currentTimeMillis());
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
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null, getTraceId(), System.currentTimeMillis());
    }

    /**
     * 失败响应（使用错误码枚举 + 自定义消息）
     *
     * @param errorCode 错误码枚举
     * @param message 自定义消息
     * @return 失败响应
     */
    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        return new R<>(errorCode.getCode(), message, null, getTraceId(), System.currentTimeMillis());
    }

    /**
     * 失败响应（自定义错误码和消息）
     *
     * @param code 错误码
     * @param message 错误消息
     * @return 失败响应
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null, getTraceId(), System.currentTimeMillis());
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

}
