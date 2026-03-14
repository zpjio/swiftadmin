package cn.swiftadmin.message.api.service;

import cn.swiftadmin.message.api.dto.MessageDTO;
import cn.swiftadmin.message.api.dto.RecordQueryDTO;
import cn.swiftadmin.message.api.vo.RecordVO;
import cn.swiftadmin.message.api.vo.SendResultVO;
import com.mybatisflex.core.paginate.Page;

import java.util.List;

/**
 * 消息发送服务接口
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface MessageService {

    /**
     * 发送消息
     *
     * @param dto 消息请求
     * @return 发送结果列表
     */
    List<SendResultVO> send(MessageDTO dto);

    /**
     * 批量发送消息
     *
     * @param messages 消息列表
     * @return 发送结果列表
     */
    List<SendResultVO> batchSend(List<MessageDTO> messages);

    /**
     * 分页查询发送记录
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<RecordVO> pageRecord(RecordQueryDTO query);

    /**
     * 获取发送记录详情
     *
     * @param id 记录ID
     * @return 记录详情
     */
    RecordVO getRecordDetail(Long id);

    /**
     * 根据消息ID获取发送记录
     *
     * @param messageId 消息ID
     * @return 记录详情
     */
    RecordVO getRecordByMessageId(String messageId);

    /**
     * 重试发送失败的消息
     *
     * @param id 记录ID
     * @return 发送结果
     */
    SendResultVO retry(Long id);
}