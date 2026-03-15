package cn.swiftadmin.message.api.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 站内信收件箱实体
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("msg_user_inbox")
public class MsgUserInbox {

    /** 主键 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 用户ID */
    @Column
    private Long userId;

    /** 消息ID */
    @Column
    private String messageId;

    /** 标题 */
    @Column
    private String title;

    /** 内容 */
    @Column
    private String content;

    /** 消息类型：system/notice/alert/promotion */
    @Column
    private String msgType;

    /** 优先级 */
    @Column
    private Integer priority;

    /** 是否已读：0=未读，1=已读 */
    @Column
    private Integer isRead;

    /** 阅读时间 */
    @Column
    private LocalDateTime readTime;

    /** 是否星标：0=否，1=是 */
    @Column
    private Integer isStar;

    /** 发送者ID */
    @Column
    private Long senderId;

    /** 发送者名称 */
    @Column
    private String senderName;

    /** 过期时间 */
    @Column
    private LocalDateTime expireTime;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;
}