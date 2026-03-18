package cn.swiftadmin.message.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.message.api.channel.ChannelSender;
import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.dto.MessageDTO;
import cn.swiftadmin.message.api.dto.RecordQueryDTO;
import cn.swiftadmin.message.api.entity.MsgChannel;
import cn.swiftadmin.message.api.entity.MsgRecord;
import cn.swiftadmin.message.api.entity.MsgTemplate;
import cn.swiftadmin.message.api.enums.ChannelType;
import cn.swiftadmin.message.api.enums.MessageStatus;
import cn.swiftadmin.message.api.service.ChannelService;
import cn.swiftadmin.message.api.service.MessageService;
import cn.swiftadmin.message.api.service.TemplateService;
import cn.swiftadmin.message.api.vo.RecordVO;
import cn.swiftadmin.message.api.vo.SendResultVO;
import cn.swiftadmin.message.channel.ChannelSenderFactory;
import cn.swiftadmin.message.mapper.MsgRecordMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.swiftadmin.message.api.entity.table.MsgRecordTableDef.MSG_RECORD;

/**
 * 消息发送服务实现
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MsgRecordMapper, MsgRecord> implements MessageService {

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;

    private final ChannelSenderFactory senderFactory;
    private final ChannelService channelService;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SendResultVO> send(MessageDTO dto) {
        // 获取模板（如果有）- 只查询一次
        MsgTemplate template = null;
        if (StringUtils.hasText(dto.getTemplateCode())) {
            template = templateService.getByCode(dto.getTemplateCode());
            if (template == null) {
                throw new BusinessException(400, "模板不存在: " + dto.getTemplateCode());
            }
        }

        // 确定要发送的渠道类型列表
        List<ChannelType> channelTypes = determineChannelTypes(dto, template);

        // 预估结果数量，避免 ArrayList 扩容
        int estimatedSize = dto.getTo().size() * channelTypes.size();
        List<SendResultVO> results = new ArrayList<>(estimatedSize);

        // 遍历每个渠道类型发送
        for (ChannelType channelType : channelTypes) {
            List<SendResultVO> channelResults = sendByChannelType(dto, channelType, template);
            results.addAll(channelResults);
        }

        return results;
    }

    /**
     * 确定要发送的渠道类型列表
     */
    private List<ChannelType> determineChannelTypes(MessageDTO dto, MsgTemplate template) {
        List<ChannelType> types = new ArrayList<>();

        // 优先使用 channelTypes（多渠道）
        if (!CollectionUtils.isEmpty(dto.getChannelTypes())) {
            for (String code : dto.getChannelTypes()) {
                types.add(parseChannelType(code));
            }
            return types;
        }

        // 单渠道模式
        if (StringUtils.hasText(dto.getChannelType())) {
            types.add(parseChannelType(dto.getChannelType()));
            return types;
        }

        // 根据模板确定渠道类型
        if (template != null) {
            ChannelType type = ChannelType.fromCode(template.getChannelType());
            if (type != null) {
                types.add(type);
                return types;
            }
        }

        // 默认使用站内信
        types.add(ChannelType.INBOX);
        return types;
    }

    /**
     * 解析渠道类型
     */
    private ChannelType parseChannelType(String code) {
        ChannelType type = ChannelType.fromCode(code);
        if (type == null) {
            throw new BusinessException(400, "无效的渠道类型: " + code);
        }
        return type;
    }

    /**
     * 按指定渠道类型发送消息
     */
    private List<SendResultVO> sendByChannelType(MessageDTO dto, ChannelType channelType, MsgTemplate template) {
        int receiverCount = dto.getTo().size();
        List<SendResultVO> results = new ArrayList<>(receiverCount);

        // 获取渠道发送器
        ChannelSender sender = senderFactory.getSender(channelType);

        // 获取渠道配置（按优先级排序）
        List<MsgChannel> channels = channelService.listEnabledByType(channelType.getCode());

        // 构建消息内容（只构建一次）
        String content = buildContent(dto, template);

        // 序列化变量（只序列化一次）
        String variablesJson = serializeVariables(dto.getVariables());

        // 构建主题（统一处理）
        String subject = dto.getSubject() != null ? dto.getSubject() :
                (template != null ? template.getSubject() : null);

        // 没有可用渠道时，统一处理所有接收者
        if (CollectionUtils.isEmpty(channels)) {
            log.warn("没有可用的{}渠道，跳过发送", channelType.getName());
            String errorMsg = "没有可用的" + channelType.getName() + "渠道";
            for (String to : dto.getTo()) {
                MsgRecord record = createRecord(dto, channelType, template, to, content, variablesJson, subject);
                record.setStatus(MessageStatus.FAILED.getCode());
                record.setErrorMsg(errorMsg);
                save(record);
                results.add(buildResultVO(record.getMessageId(), channelType.getCode(), to, false, errorMsg));
            }
            return results;
        }

        // 发送给每个接收者
        for (String to : dto.getTo()) {
            // 创建发送记录
            MsgRecord record = createRecord(dto, channelType, template, to, content, variablesJson, subject);
            save(record);

            // 尝试发送，支持故障转移到备用渠道
            SendResult sendResult = null;
            MsgChannel usedChannel = null;
            String lastError = null;

            for (MsgChannel channel : channels) {
                try {
                    usedChannel = channel;
                    record.setChannelId(channel.getId());

                    // 构建发送上下文
                    MessageContext context = MessageContext.builder()
                            .recordId(record.getId())
                            .messageId(record.getMessageId())
                            .to(to)
                            .subject(subject)
                            .content(content)
                            .variables(dto.getVariables())
                            .channel(channel)
                            .template(template)
                            .bizType(dto.getBizType())
                            .bizId(dto.getBizId())
                            .build();

                    // 发送消息
                    sendResult = sender.send(context);

                    if (sendResult.success()) {
                        break; // 发送成功，跳出循环
                    }
                    lastError = sendResult.errorMsg();
                    log.warn("渠道 {} 发送失败，尝试下一个渠道: {}", channel.getChannelCode(), lastError);
                } catch (Exception e) {
                    lastError = e.getMessage();
                    log.error("渠道 {} 发送异常: {}", channel.getChannelCode(), e.getMessage(), e);
                }
            }

            // 更新发送记录
            if (sendResult != null && sendResult.success()) {
                record.setChannelId(usedChannel.getId());
                updateRecordAfterSend(record, sendResult);
                results.add(buildResultVO(record.getMessageId(), channelType.getCode(), to, true, null));
            } else {
                // 所有渠道都失败
                String errorMsg = "所有渠道发送失败: " + lastError;
                record.setStatus(MessageStatus.FAILED.getCode());
                record.setErrorMsg(errorMsg);
                updateById(record);
                results.add(buildResultVO(record.getMessageId(), channelType.getCode(), to, false, lastError));
            }
        }

        return results;
    }

    /**
     * 构建发送结果 VO
     */
    private SendResultVO buildResultVO(String messageId, String channelType, String toAddress, boolean success, String errorMsg) {
        SendResultVO resultVO = new SendResultVO();
        resultVO.setMessageId(messageId);
        resultVO.setChannelType(channelType);
        resultVO.setToAddress(toAddress);
        resultVO.setSuccess(success);
        if (success) {
            resultVO.setSendTime(LocalDateTime.now());
        } else {
            resultVO.setErrorMsg(errorMsg);
        }
        return resultVO;
    }

    @Override
    public List<SendResultVO> batchSend(List<MessageDTO> messages) {
        List<SendResultVO> allResults = new ArrayList<>(messages.size());
        for (MessageDTO dto : messages) {
            allResults.addAll(send(dto));
        }
        return allResults;
    }

    @Override
    public Page<RecordVO> pageRecord(RecordQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(MSG_RECORD.MESSAGE_ID.eq(query.getMessageId(), query.getMessageId() != null))
                .and(MSG_RECORD.CHANNEL_TYPE.eq(query.getChannelType(), query.getChannelType() != null))
                .and(MSG_RECORD.TO_ADDRESS.like(PageHelper.likeIfPresent(query.getToAddress())))
                .and(MSG_RECORD.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .and(MSG_RECORD.BIZ_TYPE.eq(query.getBizType(), query.getBizType() != null))
                .orderBy(MSG_RECORD.ID.desc());

        if (StringUtils.hasText(query.getStartTime())) {
            LocalDateTime startTime = LocalDateTime.parse(query.getStartTime(), DateTimeFormatter.ISO_DATE_TIME);
            queryWrapper.and(MSG_RECORD.CREATE_TIME.ge(startTime));
        }
        if (StringUtils.hasText(query.getEndTime())) {
            LocalDateTime endTime = LocalDateTime.parse(query.getEndTime(), DateTimeFormatter.ISO_DATE_TIME);
            queryWrapper.and(MSG_RECORD.CREATE_TIME.le(endTime));
        }

        return mapper.paginateAs(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper, RecordVO.class);
    }

    @Override
    public RecordVO getRecordDetail(Long id) {
        MsgRecord record = getById(id);
        return record != null ? BeanCopyUtils.copy(record, RecordVO::new) : null;
    }

    @Override
    public RecordVO getRecordByMessageId(String messageId) {
        MsgRecord record = getOne(QueryWrapper.create()
                .where(MSG_RECORD.MESSAGE_ID.eq(messageId)));
        return record != null ? BeanCopyUtils.copy(record, RecordVO::new) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendResultVO retry(Long id) {
        MsgRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "发送记录不存在");
        }

        if (!MessageStatus.FAILED.getCode().equals(record.getStatus())) {
            throw new BusinessException(400, "只能重试发送失败的消息");
        }

        if (record.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new BusinessException(400, "重试次数已达上限");
        }

        // 获取渠道
        MsgChannel channel = channelService.getEntityById(record.getChannelId());
        if (channel == null || !channel.isEnabled()) {
            throw new BusinessException(400, "渠道不可用");
        }

        // 获取发送器
        ChannelType channelType = ChannelType.fromCode(record.getChannelType());
        if (channelType == null) {
            throw new BusinessException(400, "无效的渠道类型: " + record.getChannelType());
        }
        ChannelSender sender = senderFactory.getSender(channelType);

        // 构建上下文
        MessageContext context = MessageContext.builder()
                .recordId(record.getId())
                .messageId(record.getMessageId())
                .to(record.getToAddress())
                .subject(record.getSubject())
                .content(record.getContent())
                .channel(channel)
                .build();

        // 重试发送
        SendResult sendResult = sender.send(context);

        // 更新重试次数和发送结果
        record.setRetryCount(record.getRetryCount() + 1);
        updateRecordAfterSend(record, sendResult);

        return buildResultVO(record.getMessageId(), channelType.getCode(),
                record.getToAddress(), sendResult.success(),
                sendResult.success() ? null : sendResult.errorMsg());
    }

    private MsgRecord createRecord(MessageDTO dto, ChannelType channelType,
                                   MsgTemplate template, String to, String content,
                                   String variablesJson, String subject) {
        String messageId = UUID.randomUUID().toString().replace("-", "");

        return MsgRecord.builder()
                .messageId(messageId)
                .channelType(channelType.getCode())
                .templateId(template != null ? template.getId() : null)
                .templateCode(template != null ? template.getTemplateCode() : null)
                .toAddress(to)
                .subject(subject)
                .content(content)
                .variables(variablesJson)
                .bizType(dto.getBizType())
                .bizId(dto.getBizId())
                .status(MessageStatus.PENDING.getCode())
                .retryCount(0)
                .build();
    }

    /**
     * 序列化模板变量
     */
    private String serializeVariables(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            log.warn("序列化变量失败", e);
            return null;
        }
    }

    private String buildContent(MessageDTO dto, MsgTemplate template) {
        if (StringUtils.hasText(dto.getContent())) {
            return dto.getContent();
        }
        if (template != null) {
            return templateService.renderTemplate(template, dto.getVariables());
        }
        return "";
    }

    private void updateRecordAfterSend(MsgRecord record, SendResult sendResult) {
        record.setSendTime(LocalDateTime.now());

        if (sendResult.success()) {
            record.setStatus(MessageStatus.SUCCESS.getCode());
            record.setArriveTime(LocalDateTime.now());
            record.setCostTime(sendResult.costTime());
        } else {
            record.setStatus(MessageStatus.FAILED.getCode());
            record.setErrorMsg(sendResult.errorMsg());
        }

        updateById(record);
    }
}