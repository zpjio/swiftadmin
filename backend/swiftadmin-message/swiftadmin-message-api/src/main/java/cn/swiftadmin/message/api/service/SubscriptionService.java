package cn.swiftadmin.message.api.service;

import cn.swiftadmin.message.api.dto.SubscriptionDTO;
import cn.swiftadmin.message.api.vo.SubscriptionVO;

import java.util.List;

/**
 * 消息订阅服务接口
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface SubscriptionService {

    /**
     * 获取当前用户的订阅列表
     *
     * @return 订阅列表
     */
    List<SubscriptionVO> listSubscriptions();

    /**
     * 更新订阅状态
     *
     * @param dto 订阅信息
     */
    void update(SubscriptionDTO dto);

    /**
     * 批量更新订阅状态
     *
     * @param dtoList 订阅列表
     */
    void batchUpdate(List<SubscriptionDTO> dtoList);

    /**
     * 检查用户是否订阅了指定消息类型
     *
     * @param userId 用户ID
     * @param msgType 消息类型
     * @param channelType 渠道类型
     * @return true=已订阅
     */
    boolean isSubscribed(Long userId, String msgType, String channelType);

    /**
     * 初始化用户默认订阅
     *
     * @param userId 用户ID
     */
    void initDefaultSubscriptions(Long userId);
}