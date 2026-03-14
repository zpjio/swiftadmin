package cn.swiftadmin.message.api.service;

import cn.swiftadmin.message.api.dto.InboxMessageDTO;
import cn.swiftadmin.message.api.dto.InboxQueryDTO;
import cn.swiftadmin.message.api.vo.InboxVO;
import cn.swiftadmin.message.api.vo.UnreadCountVO;
import com.mybatisflex.core.paginate.Page;

import java.util.List;

/**
 * 站内信服务接口
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface InboxService {

    /**
     * 分页查询当前用户的站内信
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<InboxVO> page(InboxQueryDTO query);

    /**
     * 获取站内信详情
     *
     * @param id 消息ID
     * @return 消息详情
     */
    InboxVO getDetail(Long id);

    /**
     * 获取未读消息数量
     *
     * @return 未读数量统计
     */
    UnreadCountVO getUnreadCount();

    /**
     * 发送站内信
     *
     * @param dto 站内信信息
     * @return 发送成功数量
     */
    int send(InboxMessageDTO dto);

    /**
     * 标记消息已读
     *
     * @param ids 消息ID列表
     */
    void markRead(List<Long> ids);

    /**
     * 标记全部已读
     */
    void markAllRead();

    /**
     * 设置星标
     *
     * @param id 消息ID
     * @param isStar 是否星标
     */
    void setStar(Long id, boolean isStar);

    /**
     * 删除站内信
     *
     * @param ids 消息ID列表
     */
    void delete(List<Long> ids);

    /**
     * 清理过期消息
     *
     * @return 清理数量
     */
    int cleanExpired();
}