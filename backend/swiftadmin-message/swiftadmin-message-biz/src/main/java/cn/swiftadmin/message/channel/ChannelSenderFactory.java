package cn.swiftadmin.message.channel;

import cn.swiftadmin.message.api.channel.ChannelSender;
import cn.swiftadmin.message.api.enums.ChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 渠道发送器工厂
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Component
public class ChannelSenderFactory {

    private final Map<ChannelType, ChannelSender> senders;

    public ChannelSenderFactory(List<ChannelSender> senderList) {
        this.senders = senderList.stream()
                .collect(Collectors.toMap(ChannelSender::getChannelType, Function.identity()));
        log.info("初始化渠道发送器: {}", senders.keySet());
    }

    /**
     * 获取指定渠道类型的发送器
     *
     * @param type 渠道类型
     * @return 发送器
     */
    public ChannelSender getSender(ChannelType type) {
        ChannelSender sender = senders.get(type);
        if (sender == null) {
            throw new IllegalArgumentException("不支持的渠道类型: " + type);
        }
        return sender;
    }

    /**
     * 检查是否支持指定渠道类型
     *
     * @param type 渠道类型
     * @return 是否支持
     */
    public boolean isSupported(ChannelType type) {
        return senders.containsKey(type);
    }

    /**
     * 获取所有支持的渠道类型
     *
     * @return 渠道类型列表
     */
    public List<ChannelType> getSupportedTypes() {
        return List.copyOf(senders.keySet());
    }
}