package cn.swiftadmin.message.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户订阅 VO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class SubscriptionVO {

    /** 订阅ID */
    private Long id;

    /** 消息类型 */
    private String msgType;

    /** 渠道类型 */
    private String channelType;

    /** 是否订阅 */
    private Integer subscribed;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}