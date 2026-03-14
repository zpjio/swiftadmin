package cn.swiftadmin.message.api.dto;

import lombok.Data;

/**
 * 消息模板查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class TemplateQueryDTO {

    /** 页码 */
    private Integer page = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 模板名称 */
    private String templateName;

    /** 模板编码 */
    private String templateCode;

    /** 渠道类型 */
    private String channelType;

    /** 状态 */
    private Integer status;
}