package cn.swiftadmin.message.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** code -> ChannelType 映射缓存 */
    private static final Map<String, ChannelType> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(ChannelType::getCode, Function.identity()));

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值，不存在返回 null
     */
    public static ChannelType fromCode(String code) {
        return CODE_MAP.get(code);
    }
}