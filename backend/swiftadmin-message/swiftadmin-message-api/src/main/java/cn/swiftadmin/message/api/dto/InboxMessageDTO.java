package cn.swiftadmin.message.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送站内信 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class InboxMessageDTO {

    /** 接收用户ID列表 */
    @NotBlank(message = "接收用户不能为空")
    private String userIds;

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    /** 内容 */
    @NotBlank(message = "内容不能为空")
    private String content;

    /** 消息类型 */
    private String msgType;

    /** 优先级 */
    private Integer priority;

    /** 发送者名称 */
    @Size(max = 50, message = "发送者名称长度不能超过50")
    private String senderName;

    /** 过期时间 */
    private String expireTime;
}