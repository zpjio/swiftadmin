package cn.swiftadmin.message.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息模板 VO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class TemplateVO {

    /** 模板ID */
    private Long id;

    /** 模板名称 */
    private String templateName;

    /** 模板编码 */
    private String templateCode;

    /** 渠道类型 */
    private String channelType;

    /** 主题 */
    private String subject;

    /** 模板内容 */
    private String content;

    /** 变量列表 */
    private String variables;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建人 ID */
    private Long createBy;

    /** 更新人 ID */
    private Long updateBy;
}