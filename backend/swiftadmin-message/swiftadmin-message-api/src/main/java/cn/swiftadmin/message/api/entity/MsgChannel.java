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
 * 消息渠道配置实体
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("msg_channel")
public class MsgChannel {

    /** 主键 ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 渠道名称 */
    @Column
    private String channelName;

    /** 渠道类型：sms/email/inbox/wecom/dingtalk/feishu/webhook */
    @Column
    private String channelType;

    /** 渠道编码（唯一标识） */
    @Column
    private String channelCode;

    /** 配置信息（JSON） */
    @Column
    private String config;

    /** 状态：1=启用，0=禁用 */
    @Column
    private Integer status;

    /** 优先级（数字越大优先级越高） */
    @Column
    private Integer priority;

    /** 备注 */
    @Column
    private String remark;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column
    private LocalDateTime updateTime;

    /** 删除标记：0=正常，1=已删除 */
    @Column(isLogicDelete = true)
    private Integer deleted;
}