package cn.swiftadmin.common.core.exception;

import cn.swiftadmin.common.core.result.ErrorCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务异常基类
 * <p>增强版：支持附加上下文信息，便于问题定位和日志分析</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final int code;

    /** 附加上下文信息 */
    private final Map<String, Object> context;

    /**
     * 构造业务异常
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.context = new HashMap<>();
    }

    /**
     * 构造业务异常
     *
     * @param errorCode 错误码枚举
     * @param message 详细消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.context = new HashMap<>();
    }

    /**
     * 构造业务异常
     *
     * @param code 错误码
     * @param message 详细消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.context = new HashMap<>();
    }

    /**
     * 构造业务异常
     *
     * @param errorCode 错误码枚举
     * @param message 详细消息
     * @param cause 原因异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.context = new HashMap<>();
    }

    /**
     * 构造业务异常
     *
     * @param errorCode 错误码枚举
     * @param cause 原因异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.context = new HashMap<>();
    }

    /**
     * 构造业务异常
     *
     * @param code 错误码
     * @param message 详细消息
     * @param cause 原因异常
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.context = new HashMap<>();
    }

    /**
     * 添加上下文信息
     *
     * @param key 键
     * @param value 值
     * @return 当前异常对象（支持链式调用）
     */
    public BusinessException withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    /**
     * 批量添加上下文信息
     *
     * @param contexts 上下文 Map
     * @return 当前异常对象（支持链式调用）
     */
    public BusinessException withContext(Map<String, Object> contexts) {
        if (contexts != null) {
            this.context.putAll(contexts);
        }
        return this;
    }

    /**
     * 获取上下文信息的 JSON 字符串表示
     *
     * @return 上下文 JSON 字符串
     */
    public String getContextAsString() {
        if (context.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        context.forEach((key, value) -> {
            sb.append("\"").append(key).append("\":\"").append(value).append("\",");
        });
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 获取完整的错误信息（包含上下文）
     *
     * @return 完整错误信息
     */
    @Override
    public String getMessage() {
        String baseMessage = super.getMessage();
        if (context.isEmpty()) {
            return baseMessage;
        }
        return baseMessage + " [context: " + getContextAsString() + "]";
    }

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码枚举
     * @return 业务异常实例
     */
    public static BusinessException of(ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码枚举
     * @param message 详细消息
     * @return 业务异常实例
     */
    public static BusinessException of(ErrorCode errorCode, String message) {
        return new BusinessException(errorCode, message);
    }

    /**
     * 创建业务异常
     *
     * @param code 错误码
     * @param message 详细消息
     * @return 业务异常实例
     */
    public static BusinessException of(int code, String message) {
        return new BusinessException(code, message);
    }
}
