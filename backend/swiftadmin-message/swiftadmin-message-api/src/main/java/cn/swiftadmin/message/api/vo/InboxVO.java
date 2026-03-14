package cn.swiftadmin.message.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信 VO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class InboxVO {

    /** 消息ID */
    private Long id;

    /** 消息UUID */
    private String messageId;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 消息类型 */
    private String msgType;

    /** 优先级 */
    private Integer priority;

    /** 是否已读 */
    private Integer isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 是否星标 */
    private Integer isStar;

    /** 发送者ID */
    private Long senderId;

    /** 发送者名称 */
    private String senderName;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}