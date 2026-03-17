package cn.swiftadmin.message.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息发送结果 VO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class SendResultVO {

    /** 是否成功 */
    private Boolean success;

    /** 消息ID */
    private String messageId;

    /** 渠道类型 */
    private String channelType;

    /** 接收地址 */
    private String toAddress;

    /** 错误信息 */
    private String errorMsg;

    /** 发送时间 */
    private LocalDateTime sendTime;

    public static SendResultVO success(String messageId, String channelType, String toAddress) {
        SendResultVO result = new SendResultVO();
        result.setSuccess(true);
        result.setMessageId(messageId);
        result.setChannelType(channelType);
        result.setToAddress(toAddress);
        result.setSendTime(LocalDateTime.now());
        return result;
    }

    public static SendResultVO fail(String channelType, String toAddress, String errorMsg) {
        SendResultVO result = new SendResultVO();
        result.setSuccess(false);
        result.setChannelType(channelType);
        result.setToAddress(toAddress);
        result.setErrorMsg(errorMsg);
        return result;
    }
}