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
 * 消息发送记录实体
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("msg_record")
public class MsgRecord {

    /** 主键 ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 消息ID（UUID） */
    @Column
    private String messageId;

    /** 渠道类型 */
    @Column
    private String channelType;

    /** 渠道ID */
    @Column
    private Long channelId;

    /** 模板ID */
    @Column
    private Long templateId;

    /** 模板编码 */
    @Column
    private String templateCode;

    /** 接收地址 */
    @Column
    private String toAddress;

    /** 主题 */
    @Column
    private String subject;

    /** 消息内容 */
    @Column
    private String content;

    /** 模板变量（JSON） */
    @Column
    private String variables;

    /** 业务类型 */
    @Column
    private String bizType;

    /** 业务ID */
    @Column
    private String bizId;

    /** 状态：pending/sending/success/failed */
    @Column
    private String status;

    /** 错误信息 */
    @Column
    private String errorMsg;

    /** 重试次数 */
    @Column
    private Integer retryCount;

    /** 发送时间 */
    @Column
    private LocalDateTime sendTime;

    /** 到达时间 */
    @Column
    private LocalDateTime arriveTime;

    /** 耗时（毫秒） */
    @Column
    private Integer costTime;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;
}