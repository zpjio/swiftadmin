package cn.swiftadmin.message.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量发送消息请求 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class BatchMessageDTO {

    /** 消息列表 */
    private List<MessageDTO> messages;
}