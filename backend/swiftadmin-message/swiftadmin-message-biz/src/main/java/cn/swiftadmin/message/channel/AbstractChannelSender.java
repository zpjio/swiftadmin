package cn.swiftadmin.message.channel;

import cn.swiftadmin.message.api.channel.ChannelSender;
import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.enums.ChannelType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 渠道发送器抽象基类
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
public abstract class AbstractChannelSender implements ChannelSender {

    @Override
    public SendResult send(MessageContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("发送消息: channelType={}, to={}", getChannelType(), context.getTo());
            SendResult result = doSend(context);
            long costTime = System.currentTimeMillis() - startTime;
            log.info("消息发送完成: channelType={}, to={}, success={}, cost={}ms",
                    getChannelType(), context.getTo(), result.success(), costTime);
            return result;
        } catch (Exception e) {
            log.error("消息发送异常: channelType={}, to={}", getChannelType(), context.getTo(), e);
            return SendResult.fail(context.getTo(), e.getMessage());
        }
    }

    @Override
    public List<SendResult> batchSend(List<MessageContext> contexts) {
        List<SendResult> results = new ArrayList<>();
        for (MessageContext context : contexts) {
            results.add(send(context));
        }
        return results;
    }

    @Override
    public boolean healthCheck(String config) {
        try {
            return doHealthCheck(config);
        } catch (Exception e) {
            log.error("健康检查异常: channelType={}", getChannelType(), e);
            return false;
        }
    }

    /**
     * 实际发送逻辑
     *
     * @param context 消息上下文
     * @return 发送结果
     */
    protected abstract SendResult doSend(MessageContext context);

    /**
     * 实际健康检查逻辑
     *
     * @param config 渠道配置JSON
     * @return 是否健康
     */
    protected boolean doHealthCheck(String config) {
        return true;
    }
}