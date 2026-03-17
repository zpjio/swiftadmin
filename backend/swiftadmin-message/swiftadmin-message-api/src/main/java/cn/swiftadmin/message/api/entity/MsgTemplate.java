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
 * 消息模板实体
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("msg_template")
public class MsgTemplate {

    /** 主键 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 模板名称 */
    @Column
    private String templateName;

    /** 模板编码（系统内部唯一标识，业务代码引用） */
    @Column
    private String templateCode;

    /** 云服务商模板码（如阿里云 SMS_123456789，腾讯云 123456） */
    @Column
    private String providerTemplateCode;

    /** 渠道类型 */
    @Column
    private String channelType;

    /** 主题（邮件使用） */
    @Column
    private String subject;

    /** 模板内容 */
    @Column
    private String content;

    /** 变量列表（JSON数组） */
    @Column
    private String variables;

    /** 状态：0=禁用，1=启用，2=待审核 */
    @Column
    private Integer status;

    /** 备注 */
    @Column
    private String remark;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column
    private LocalDateTime updateTime;

    /** 创建人 ID */
    @Column
    private Long createBy;

    /** 更新人 ID */
    @Column
    private Long updateBy;

    /** 删除标记：0=正常，1=已删除 */
    @Column(isLogicDelete = true)
    private Integer deleted;
}