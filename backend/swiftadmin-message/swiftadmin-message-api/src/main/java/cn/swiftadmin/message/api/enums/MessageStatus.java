package cn.swiftadmin.message.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息状态枚举
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Getter
@RequiredArgsConstructor
public enum MessageStatus {

    /** 待发送 */
    PENDING("pending", "待发送"),

    /** 发送中 */
    SENDING("sending", "发送中"),

    /** 发送成功 */
    SUCCESS("success", "发送成功"),

    /** 发送失败 */
    FAILED("failed", "发送失败");

    @JsonValue
    private final String code;

    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值
     */
    public static MessageStatus fromCode(String code) {
        for (MessageStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}