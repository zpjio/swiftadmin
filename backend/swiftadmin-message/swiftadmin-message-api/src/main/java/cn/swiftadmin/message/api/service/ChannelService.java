package cn.swiftadmin.message.api.service;

import cn.swiftadmin.message.api.dto.ChannelDTO;
import cn.swiftadmin.message.api.dto.ChannelQueryDTO;
import cn.swiftadmin.message.api.entity.MsgChannel;
import cn.swiftadmin.message.api.vo.ChannelVO;
import com.mybatisflex.core.paginate.Page;

import java.util.List;

/**
 * 消息渠道服务接口
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface ChannelService {

    /**
     * 分页查询渠道
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<ChannelVO> page(ChannelQueryDTO query);

    /**
     * 获取渠道详情
     *
     * @param id 渠道ID
     * @return 渠道详情
     */
    ChannelVO getDetail(Long id);

    /**
     * 根据渠道编码获取渠道
     *
     * @param channelCode 渠道编码
     * @return 渠道实体
     */
    MsgChannel getByCode(String channelCode);

    /**
     * 获取指定类型的启用渠道列表
     *
     * @param channelType 渠道类型
     * @return 渠道列表
     */
    List<MsgChannel> listEnabledByType(String channelType);

    /**
     * 获取所有启用的渠道列表
     *
     * @return 渠道列表
     */
    List<MsgChannel> listAllEnabled();

    /**
     * 创建渠道
     *
     * @param dto 渠道信息
     * @return 渠道ID
     */
    Long create(ChannelDTO dto);

    /**
     * 更新渠道
     *
     * @param dto 渠道信息
     */
    void update(ChannelDTO dto);

    /**
     * 删除渠道
     *
     * @param ids 渠道ID列表
     */
    void delete(List<Long> ids);

    /**
     * 更新渠道状态
     *
     * @param id 渠道ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 测试渠道连接
     *
     * @param id 渠道ID
     * @return 测试结果
     */
    boolean testConnection(Long id);

    /**
     * 检查渠道编码是否存在
     *
     * @param channelCode 渠道编码
     * @param excludeId 排除的ID
     * @return true=存在
     */
    boolean existsByCode(String channelCode, Long excludeId);
}