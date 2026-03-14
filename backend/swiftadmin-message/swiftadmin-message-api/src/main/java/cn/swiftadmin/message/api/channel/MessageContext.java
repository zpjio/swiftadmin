package cn.swiftadmin.message.api.channel;

import cn.swiftadmin.message.api.entity.MsgChannel;
import cn.swiftadmin.message.api.entity.MsgTemplate;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 消息发送上下文
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
@Builder
public class MessageContext {

    /** 记录ID */
    private Long recordId;

    /** 消息ID */
    private String messageId;

    /** 接收者 */
    private String to;

    /** 主题 */
    private String subject;

    /** 内容 */
    private String content;

    /** 模板变量 */
    private Map<String, Object> variables;

    /** 渠道配置 */
    private MsgChannel channel;

    /** 模板（可选） */
    private MsgTemplate template;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private String bizId;
}