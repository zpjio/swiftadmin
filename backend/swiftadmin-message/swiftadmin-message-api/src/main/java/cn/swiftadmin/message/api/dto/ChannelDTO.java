package cn.swiftadmin.message.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 消息渠道 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class ChannelDTO {

    /** 渠道ID */
    private Long id;

    /** 渠道名称 */
    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 50, message = "渠道名称长度不能超过50")
    private String channelName;

    /** 渠道类型 */
    @NotBlank(message = "渠道类型不能为空")
    private String channelType;

    /** 渠道编码 */
    @NotBlank(message = "渠道编码不能为空")
    @Size(max = 50, message = "渠道编码长度不能超过50")
    private String channelCode;

    /** 配置信息（JSON） */
    @NotBlank(message = "配置信息不能为空")
    private String config;

    /** 状态：1=启用，0=禁用 */
    private Integer status;

    /** 优先级 */
    private Integer priority;

    /** 备注 */
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}