package cn.swiftadmin.message.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.message.api.channel.ChannelSender;
import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.dto.InboxMessageDTO;
import cn.swiftadmin.message.api.dto.InboxQueryDTO;
import cn.swiftadmin.message.api.entity.MsgChannel;
import cn.swiftadmin.message.api.entity.MsgRecord;
import cn.swiftadmin.message.api.entity.MsgUserInbox;
import cn.swiftadmin.message.api.enums.ChannelType;
import cn.swiftadmin.message.api.service.InboxService;
import cn.swiftadmin.message.api.vo.InboxVO;
import cn.swiftadmin.message.api.vo.UnreadCountVO;
import cn.swiftadmin.message.mapper.MsgRecordMapper;
import cn.swiftadmin.message.mapper.MsgUserInboxMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static cn.swiftadmin.message.api.entity.table.MsgUserInboxTableDef.MSG_USER_INBOX;

/**
 * 站内信服务实现
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxServiceImpl extends ServiceImpl<MsgUserInboxMapper, MsgUserInbox> implements InboxService {

    private final ObjectMapper objectMapper;
    private final MsgRecordMapper msgRecordMapper;

    @Override
    public Page<InboxVO> page(InboxQueryDTO query) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(MSG_USER_INBOX.USER_ID.eq(userId))
                .and(MSG_USER_INBOX.IS_READ.eq(query.getIsRead(), query.getIsRead() != null))
                .and(MSG_USER_INBOX.MSG_TYPE.eq(query.getMsgType(), query.getMsgType() != null))
                .and(MSG_USER_INBOX.IS_STAR.eq(query.getIsStar(), query.getIsStar() != null))
                .and(MSG_USER_INBOX.TITLE.like(PageHelper.likeIfPresent(query.getKeyword()))
                        .or(MSG_USER_INBOX.CONTENT.like(PageHelper.likeIfPresent(query.getKeyword()))))
                .orderBy(MSG_USER_INBOX.IS_READ.asc(), MSG_USER_INBOX.CREATE_TIME.desc());

        return mapper.paginateAs(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper, InboxVO.class);
    }

    @Override
    public InboxVO getDetail(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        MsgUserInbox inbox = getOne(QueryWrapper.create()
                .where(MSG_USER_INBOX.ID.eq(id))
                .and(MSG_USER_INBOX.USER_ID.eq(userId)));

        if (inbox == null) {
            return null;
        }

        // 自动标记为已读
        if (inbox.getIsRead() == 0) {
            markRead(Collections.singletonList(id));
        }

        return BeanCopyUtils.copy(inbox, InboxVO::new);
    }

    @Override
    public UnreadCountVO getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return UnreadCountVO.of(0L, Collections.emptyMap());
        }

        // 一次查询获取所有未读消息，在内存中统计（减少数据库查询次数）
        List<MsgUserInbox> unreadList = list(QueryWrapper.create()
                .select(MSG_USER_INBOX.MSG_TYPE)
                .where(MSG_USER_INBOX.USER_ID.eq(userId))
                .and(MSG_USER_INBOX.IS_READ.eq(0)));

        if (CollectionUtils.isEmpty(unreadList)) {
            return UnreadCountVO.of(0L, Collections.emptyMap());
        }

        long total = unreadList.size();
        Map<String, Long> byType = unreadList.stream()
                .filter(inbox -> StringUtils.hasText(inbox.getMsgType()))
                .collect(Collectors.groupingBy(MsgUserInbox::getMsgType, Collectors.counting()));

        return UnreadCountVO.of(total, byType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int send(InboxMessageDTO dto) {
        // 解析用户ID列表
        List<Long> userIds;
        try {
            userIds = Arrays.stream(dto.getUserIds().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "用户ID格式错误");
        }

        if (CollectionUtils.isEmpty(userIds)) {
            throw new BusinessException(400, "接收用户不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = null;
        if (StringUtils.hasText(dto.getExpireTime())) {
            try {
                expireTime = LocalDateTime.parse(dto.getExpireTime());
            } catch (Exception e) {
                log.warn("过期时间格式错误: {}", dto.getExpireTime());
            }
        }

        Long senderId = SecurityUtils.getCurrentUserId();
        String senderName = StringUtils.hasText(dto.getSenderName()) ? dto.getSenderName() : "系统管理员";

        List<MsgUserInbox> inboxList = new ArrayList<>();
        List<MsgRecord> records = new ArrayList<>();

        for (Long userId : userIds) {
            // 每条消息生成唯一messageId
            String messageId = UUID.randomUUID().toString().replace("-", "");

            MsgUserInbox inbox = MsgUserInbox.builder()
                    .userId(userId)
                    .messageId(messageId)
                    .title(dto.getTitle())
                    .content(dto.getContent())
                    .msgType(dto.getMsgType() != null ? dto.getMsgType() : "system")
                    .priority(dto.getPriority() != null ? dto.getPriority() : 0)
                    .isRead(0)
                    .isStar(0)
                    .senderId(senderId)
                    .senderName(senderName)
                    .expireTime(expireTime)
                    .build();
            inboxList.add(inbox);

            // 创建发送记录
            MsgRecord record = MsgRecord.builder()
                    .messageId(messageId)
                    .channelType("inbox")
                    .toAddress(String.valueOf(userId))
                    .subject(dto.getTitle())
                    .content(dto.getContent())
                    .bizType(dto.getMsgType())
                    .status("success")
                    .retryCount(0)
                    .sendTime(now)
                    .arriveTime(now)
                    .costTime(0)
                    .build();
            records.add(record);
        }

        saveBatch(inboxList);
        msgRecordMapper.insertBatch(records);

        log.info("站内信发送成功: count={}", inboxList.size());
        return inboxList.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return;
        }

        UpdateChain.of(MsgUserInbox.class)
                .set(MSG_USER_INBOX.IS_READ, 1)
                .set(MSG_USER_INBOX.READ_TIME, LocalDateTime.now())
                .where(MSG_USER_INBOX.ID.in(ids))
                .and(MSG_USER_INBOX.USER_ID.eq(userId))
                .update();

        log.info("站内信标记已读: userId={}, ids={}", userId, ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return;
        }

        UpdateChain.of(MsgUserInbox.class)
                .set(MSG_USER_INBOX.IS_READ, 1)
                .set(MSG_USER_INBOX.READ_TIME, LocalDateTime.now())
                .where(MSG_USER_INBOX.USER_ID.eq(userId))
                .and(MSG_USER_INBOX.IS_READ.eq(0))
                .update();

        log.info("站内信全部标记已读: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setStar(Long id, boolean isStar) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return;
        }

        UpdateChain.of(MsgUserInbox.class)
                .set(MSG_USER_INBOX.IS_STAR, isStar ? 1 : 0)
                .where(MSG_USER_INBOX.ID.eq(id))
                .and(MSG_USER_INBOX.USER_ID.eq(userId))
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return;
        }

        mapper.deleteByQuery(QueryWrapper.create()
                .where(MSG_USER_INBOX.ID.in(ids))
                .and(MSG_USER_INBOX.USER_ID.eq(userId)));

        log.info("站内信删除: userId={}, ids={}", userId, ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpired() {
        LocalDateTime now = LocalDateTime.now();
        int count = mapper.deleteByQuery(QueryWrapper.create()
                .where(MSG_USER_INBOX.EXPIRE_TIME.isNotNull())
                .and(MSG_USER_INBOX.EXPIRE_TIME.lt(now)));

        log.info("清理过期站内信: count={}", count);
        return count;
    }
}