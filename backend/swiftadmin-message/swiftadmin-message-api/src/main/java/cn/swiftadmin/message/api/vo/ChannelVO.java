package cn.swiftadmin.message.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息渠道 VO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class ChannelVO {

    /** 渠道ID */
    private Long id;

    /** 渠道名称 */
    private String channelName;

    /** 渠道类型 */
    private String channelType;

    /** 渠道编码 */
    private String channelCode;

    /** 配置信息 */
    private String config;

    /** 状态 */
    private Integer status;

    /** 优先级 */
    private Integer priority;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}