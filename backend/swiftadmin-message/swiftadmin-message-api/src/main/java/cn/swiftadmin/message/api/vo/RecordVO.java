package cn.swiftadmin.message.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息发送记录 VO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class RecordVO {

    /** 记录ID */
    private Long id;

    /** 消息ID */
    private String messageId;

    /** 渠道类型 */
    private String channelType;

    /** 渠道ID */
    private Long channelId;

    /** 模板ID */
    private Long templateId;

    /** 模板编码 */
    private String templateCode;

    /** 接收地址 */
    private String toAddress;

    /** 主题 */
    private String subject;

    /** 消息内容 */
    private String content;

    /** 模板变量 */
    private String variables;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private String bizId;

    /** 状态 */
    private String status;

    /** 错误信息 */
    private String errorMsg;

    /** 重试次数 */
    private Integer retryCount;

    /** 发送时间 */
    private LocalDateTime sendTime;

    /** 到达时间 */
    private LocalDateTime arriveTime;

    /** 耗时（毫秒） */
    private Integer costTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}