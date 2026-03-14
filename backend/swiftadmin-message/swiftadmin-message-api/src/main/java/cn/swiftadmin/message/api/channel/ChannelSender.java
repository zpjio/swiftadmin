package cn.swiftadmin.message.api.channel;

import cn.swiftadmin.message.api.enums.ChannelType;

import java.util.List;

/**
 * 渠道发送器接口
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface ChannelSender {

    /**
     * 获取渠道类型
     *
     * @return 渠道类型
     */
    ChannelType getChannelType();

    /**
     * 发送消息
     *
     * @param context 消息上下文
     * @return 发送结果
     */
    SendResult send(MessageContext context);

    /**
     * 批量发送消息
     *
     * @param contexts 消息上下文列表
     * @return 发送结果列表
     */
    List<SendResult> batchSend(List<MessageContext> contexts);

    /**
     * 健康检查
     *
     * @param config 渠道配置JSON
     * @return 是否健康
     */
    boolean healthCheck(String config);

    /**
     * 测试发送
     *
     * @param config 渠道配置JSON
     * @param to 测试接收地址
     * @return 测试结果
     */
    default SendResult testSend(String config, String to) {
        return SendResult.fail(to, "该渠道不支持测试发送");
    }
}