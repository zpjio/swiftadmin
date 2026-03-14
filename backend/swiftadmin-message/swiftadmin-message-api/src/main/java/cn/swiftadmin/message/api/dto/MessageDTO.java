package cn.swiftadmin.message.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 发送消息请求 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class MessageDTO {

    /** 接收者列表 */
    @NotEmpty(message = "接收者不能为空")
    @Size(max = 100, message = "单次发送接收者数量不能超过100")
    private List<String> to;

    /** 渠道类型（可选，不填则用默认渠道） */
    private String channelType;

    /** 模板编码（可选） */
    private String templateCode;

    /** 主题（可选） */
    @Size(max = 200, message = "主题长度不能超过200")
    private String subject;

    /** 内容（与模板二选一） */
    private String content;

    /** 模板变量 */
    private Map<String, Object> variables;

    /** 业务类型 */
    @Size(max = 50, message = "业务类型长度不能超过50")
    private String bizType;

    /** 业务ID */
    @Size(max = 100, message = "业务ID长度不能超过100")
    private String bizId;
}