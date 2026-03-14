package cn.swiftadmin.message.api.dto;

import lombok.Data;

/**
 * 消息渠道查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class ChannelQueryDTO {

    /** 页码 */
    private Integer page = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 渠道名称 */
    private String channelName;

    /** 渠道类型 */
    private String channelType;

    /** 状态 */
    private Integer status;
}