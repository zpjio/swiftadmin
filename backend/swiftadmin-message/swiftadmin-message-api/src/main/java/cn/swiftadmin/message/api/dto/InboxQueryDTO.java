package cn.swiftadmin.message.api.dto;

import lombok.Data;

/**
 * 站内信查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class InboxQueryDTO {

    /** 页码 */
    private Integer page = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 是否已读 */
    private Integer isRead;

    /** 消息类型 */
    private String msgType;

    /** 是否星标 */
    private Integer isStar;

    /** 关键词（标题/内容） */
    private String keyword;
}