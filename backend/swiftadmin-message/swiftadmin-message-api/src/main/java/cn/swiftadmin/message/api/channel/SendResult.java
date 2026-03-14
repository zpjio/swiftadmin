package cn.swiftadmin.message.api.channel;

import cn.swiftadmin.message.api.entity.MsgChannel;
import cn.swiftadmin.message.api.entity.MsgTemplate;

import java.util.Map;

/**
 * 消息发送结果
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public record SendResult(
        boolean success,
        String messageId,
        String toAddress,
        String errorMsg,
        Integer costTime
) {
    public static SendResult success(String messageId, String toAddress, Integer costTime) {
        return new SendResult(true, messageId, toAddress, null, costTime);
    }

    public static SendResult fail(String toAddress, String errorMsg) {
        return new SendResult(false, null, toAddress, errorMsg, null);
    }
}