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
 * 用户消息订阅实体
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("msg_subscription")
public class MsgSubscription {

    /** 主键 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 用户ID */
    @Column
    private Long userId;

    /** 消息类型 */
    @Column
    private String msgType;

    /** 渠道类型 */
    @Column
    private String channelType;

    /** 是否订阅：1=订阅，0=取消 */
    @Column
    private Integer subscribed;

    /** 创建时间 */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}