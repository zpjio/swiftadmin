package cn.swiftadmin.message.channel;

import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.entity.MsgUserInbox;
import cn.swiftadmin.message.api.enums.ChannelType;
import cn.swiftadmin.message.mapper.MsgUserInboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 站内信渠道发送器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InboxChannelSender extends AbstractChannelSender {

    private final MsgUserInboxMapper msgUserInboxMapper;

    @Override
    public ChannelType getChannelType() {
        return ChannelType.INBOX;
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        try {
            // 解析用户ID
            Long userId;
            try {
                userId = Long.parseLong(context.getTo());
            } catch (NumberFormatException e) {
                log.error("无效的用户ID: {}", context.getTo());
                return SendResult.fail(context.getTo(), "无效的用户ID: " + context.getTo());
            }

            // 生成消息ID
            String messageId = context.getMessageId() != null
                    ? context.getMessageId()
                    : UUID.randomUUID().toString().replace("-", "");

            // 创建站内信记录
            MsgUserInbox inbox = MsgUserInbox.builder()
                    .userId(userId)
                    .messageId(messageId)
                    .title(context.getSubject() != null ? context.getSubject() : "系统消息")
                    .content(context.getContent())
                    .msgType(context.getBizType() != null ? context.getBizType() : "system")
                    .priority(0)
                    .isRead(0)
                    .isStar(0)
                    .senderName("系统")
                    .createTime(LocalDateTime.now())
                    .build();

            msgUserInboxMapper.insert(inbox);

            log.info("站内信发送成功: userId={}, messageId={}", userId, messageId);
            return SendResult.success(messageId, context.getTo(), 0);
        } catch (Exception e) {
            log.error("站内信发送失败: to={}", context.getTo(), e);
            return SendResult.fail(context.getTo(), e.getMessage());
        }
    }

    @Override
    protected boolean doHealthCheck(String config) {
        // 站内信始终健康
        return true;
    }
}