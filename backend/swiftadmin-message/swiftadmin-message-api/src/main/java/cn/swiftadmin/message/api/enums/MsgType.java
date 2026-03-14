package cn.swiftadmin.message.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息类型枚举
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Getter
@RequiredArgsConstructor
public enum MsgType {

    /** 系统通知 */
    SYSTEM("system", "系统通知"),

    /** 安全提醒 */
    SECURITY("security", "安全提醒"),

    /** 订单通知 */
    ORDER("order", "订单通知"),

    /** 营销活动 */
    PROMOTION("promotion", "营销活动"),

    /** 任务提醒 */
    TASK("task", "任务提醒"),

    /** 公告通知 */
    NOTICE("notice", "公告通知"),

    /** 告警通知 */
    ALERT("alert", "告警通知");

    @JsonValue
    private final String code;

    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值
     */
    public static MsgType fromCode(String code) {
        for (MsgType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}