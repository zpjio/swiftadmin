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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    private final ChannelSenderFactory senderFactory;
    private final ChannelService channelService;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SendResultVO> send(MessageDTO dto) {
        List<SendResultVO> results = new ArrayList<>();

        // 获取渠道类型
        ChannelType channelType = determineChannelType(dto);

        // 获取渠道发送器
        ChannelSender sender = senderFactory.getSender(channelType);

        // 获取模板（如果有）
        MsgTemplate template = null;
        if (StringUtils.hasText(dto.getTemplateCode())) {
            template = templateService.getByCode(dto.getTemplateCode());
            if (template == null) {
                throw new BusinessException(400, "模板不存在: " + dto.getTemplateCode());
            }
        }

        // 获取渠道配置
        List<MsgChannel> channels = channelService.listEnabledByType(channelType.getCode());
        if (CollectionUtils.isEmpty(channels)) {
            throw new BusinessException(400, "没有可用的" + channelType.getName() + "渠道");
        }
        MsgChannel channel = channels.get(0); // 使用优先级最高的渠道

        // 发送给每个接收者
        for (String to : dto.getTo()) {
            // 创建发送记录
            MsgRecord record = createRecord(dto, channelType, channel, template, to);
            save(record);

            // 构建消息内容
            String content = buildContent(dto, template);

            // 构建发送上下文
            MessageContext context = MessageContext.builder()
                    .recordId(record.getId())
                    .messageId(record.getMessageId())
                    .to(to)
                    .subject(dto.getSubject() != null ? dto.getSubject() :
                            (template != null ? template.getSubject() : null))
                    .content(content)
                    .variables(dto.getVariables())
                    .channel(channel)
                    .template(template)
                    .bizType(dto.getBizType())
                    .bizId(dto.getBizId())
                    .build();

            // 发送消息
            SendResult sendResult = sender.send(context);

            // 更新发送记录
            updateRecordAfterSend(record.getId(), sendResult);

            // 构建返回结果
            SendResultVO resultVO = new SendResultVO();
            resultVO.setMessageId(record.getMessageId());
            resultVO.setToAddress(to);
            if (sendResult.success()) {
                resultVO.setSuccess(true);
                resultVO.setSendTime(LocalDateTime.now());
            } else {
                resultVO.setSuccess(false);
                resultVO.setErrorMsg(sendResult.errorMsg());
            }
            results.add(resultVO);
        }

        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SendResultVO> batchSend(List<MessageDTO> messages) {
        List<SendResultVO> allResults = new ArrayList<>();
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

        if (record.getRetryCount() >= 3) {
            throw new BusinessException(400, "重试次数已达上限");
        }

        // 获取渠道
        MsgChannel channel = channelService.getByCode(record.getChannelType());
        if (channel == null || channel.getStatus() != 1) {
            throw new BusinessException(400, "渠道不可用");
        }

        // 获取发送器
        ChannelType channelType = ChannelType.fromCode(record.getChannelType());
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

        // 更新记录
        record.setRetryCount(record.getRetryCount() + 1);
        updateRecordAfterSend(id, sendResult);

        SendResultVO resultVO = new SendResultVO();
        resultVO.setMessageId(record.getMessageId());
        resultVO.setToAddress(record.getToAddress());
        resultVO.setSuccess(sendResult.success());
        if (!sendResult.success()) {
            resultVO.setErrorMsg(sendResult.errorMsg());
        }
        return resultVO;
    }

    private ChannelType determineChannelType(MessageDTO dto) {
        if (StringUtils.hasText(dto.getChannelType())) {
            ChannelType type = ChannelType.fromCode(dto.getChannelType());
            if (type == null) {
                throw new BusinessException(400, "无效的渠道类型: " + dto.getChannelType());
            }
            return type;
        }

        // 如果没有指定渠道类型，根据模板或默认使用站内信
        if (StringUtils.hasText(dto.getTemplateCode())) {
            MsgTemplate template = templateService.getByCode(dto.getTemplateCode());
            if (template != null) {
                ChannelType type = ChannelType.fromCode(template.getChannelType());
                if (type != null) {
                    return type;
                }
            }
        }

        // 默认使用站内信
        return ChannelType.INBOX;
    }

    private MsgRecord createRecord(MessageDTO dto, ChannelType channelType, MsgChannel channel,
                                   MsgTemplate template, String to) {
        String messageId = UUID.randomUUID().toString().replace("-", "");
        String variablesJson = null;
        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            try {
                variablesJson = objectMapper.writeValueAsString(dto.getVariables());
            } catch (JsonProcessingException e) {
                log.warn("序列化变量失败", e);
            }
        }

        return MsgRecord.builder()
                .messageId(messageId)
                .channelType(channelType.getCode())
                .channelId(channel.getId())
                .templateId(template != null ? template.getId() : null)
                .templateCode(template != null ? template.getTemplateCode() : null)
                .toAddress(to)
                .subject(dto.getSubject())
                .content(dto.getContent())
                .variables(variablesJson)
                .bizType(dto.getBizType())
                .bizId(dto.getBizId())
                .status(MessageStatus.PENDING.getCode())
                .retryCount(0)
                .createTime(LocalDateTime.now())
                .build();
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

    private void updateRecordAfterSend(Long recordId, SendResult sendResult) {
        MsgRecord record = new MsgRecord();
        record.setId(recordId);
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