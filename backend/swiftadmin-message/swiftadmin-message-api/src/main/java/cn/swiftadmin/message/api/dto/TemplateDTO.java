package cn.swiftadmin.message.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 消息模板 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class TemplateDTO {

    /** 模板ID */
    private Long id;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String templateName;

    /** 模板编码 */
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50, message = "模板编码长度不能超过50")
    private String templateCode;

    /** 渠道类型 */
    @NotBlank(message = "渠道类型不能为空")
    private String channelType;

    /** 主题（邮件使用） */
    @Size(max = 200, message = "主题长度不能超过200")
    private String subject;

    /** 模板内容 */
    @NotBlank(message = "模板内容不能为空")
    private String content;

    /** 变量列表（JSON数组） */
    @Size(max = 500, message = "变量列表长度不能超过500")
    private String variables;

    /** 状态：0=禁用，1=启用，2=待审核 */
    private Integer status;

    /** 备注 */
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}