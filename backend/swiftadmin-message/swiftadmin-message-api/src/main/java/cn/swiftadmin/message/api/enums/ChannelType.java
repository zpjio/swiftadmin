package cn.swiftadmin.message.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息渠道类型枚举
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Getter
@RequiredArgsConstructor
public enum ChannelType {

    /** 短信 */
    SMS("sms", "短信"),

    /** 邮件 */
    EMAIL("email", "邮件"),

    /** 站内信 */
    INBOX("inbox", "站内信"),

    /** 企业微信 */
    WECOM("wecom", "企业微信"),

    /** 钉钉 */
    DINGTALK("dingtalk", "钉钉"),

    /** 飞书 */
    FEISHU("feishu", "飞书"),

    /** Webhook */
    WEBHOOK("webhook", "Webhook");

    @JsonValue
    private final String code;

    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值
     */
    public static ChannelType fromCode(String code) {
        for (ChannelType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}