package cn.swiftadmin.message.api.vo;

import lombok.Data;

/**
 * 未读消息统计 VO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class UnreadCountVO {

    /** 未读总数 */
    private Long total;

    /** 按消息类型统计 */
    private java.util.Map<String, Long> byType;

    public static UnreadCountVO of(Long total, java.util.Map<String, Long> byType) {
        UnreadCountVO vo = new UnreadCountVO();
        vo.setTotal(total);
        vo.setByType(byType);
        return vo;
    }
}