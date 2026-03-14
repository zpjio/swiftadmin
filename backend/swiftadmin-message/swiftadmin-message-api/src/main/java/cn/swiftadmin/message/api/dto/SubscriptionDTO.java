package cn.swiftadmin.message.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户订阅 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class SubscriptionDTO {

    /** 订阅ID */
    private Long id;

    /** 消息类型 */
    @NotNull(message = "消息类型不能为空")
    private String msgType;

    /** 渠道类型 */
    @NotNull(message = "渠道类型不能为空")
    private String channelType;

    /** 是否订阅：1=订阅，0=取消 */
    @NotNull(message = "订阅状态不能为空")
    private Integer subscribed;
}