package cn.swiftadmin.message.api.dto;

import lombok.Data;

/**
 * 消息发送记录查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class RecordQueryDTO {

    /** 页码 */
    private Integer page = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 消息ID */
    private String messageId;

    /** 渠道类型 */
    private String channelType;

    /** 接收地址 */
    private String toAddress;

    /** 状态 */
    private String status;

    /** 业务类型 */
    private String bizType;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;
}