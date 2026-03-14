package cn.swiftadmin.message.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.message.api.dto.SubscriptionDTO;
import cn.swiftadmin.message.api.entity.MsgSubscription;
import cn.swiftadmin.message.api.service.SubscriptionService;
import cn.swiftadmin.message.api.vo.SubscriptionVO;
import cn.swiftadmin.message.mapper.MsgSubscriptionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

import static cn.swiftadmin.message.api.entity.table.MsgSubscriptionTableDef.MSG_SUBSCRIPTION;

/**
 * 消息订阅服务实现
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl extends ServiceImpl<MsgSubscriptionMapper, MsgSubscription> implements SubscriptionService {

    /** 默认消息类型 */
    private static final String[] DEFAULT_MSG_TYPES = {"system", "security", "order", "promotion", "task", "notice", "alert"};

    /** 默认渠道 */
    private static final String DEFAULT_CHANNEL = "inbox";

    @Override
    public List<SubscriptionVO> listSubscriptions() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        return BeanCopyUtils.copyList(
                list(QueryWrapper.create()
                        .where(MSG_SUBSCRIPTION.USER_ID.eq(userId))),
                SubscriptionVO::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SubscriptionDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        MsgSubscription existing = getOne(QueryWrapper.create()
                .where(MSG_SUBSCRIPTION.USER_ID.eq(userId))
                .and(MSG_SUBSCRIPTION.MSG_TYPE.eq(dto.getMsgType()))
                .and(MSG_SUBSCRIPTION.CHANNEL_TYPE.eq(dto.getChannelType())));

        if (existing != null) {
            existing.setSubscribed(dto.getSubscribed());
            existing.setUpdateTime(LocalDateTime.now());
            updateById(existing);
        } else {
            MsgSubscription subscription = MsgSubscription.builder()
                    .userId(userId)
                    .msgType(dto.getMsgType())
                    .channelType(dto.getChannelType())
                    .subscribed(dto.getSubscribed())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            save(subscription);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<SubscriptionDTO> dtoList) {
        if (CollectionUtils.isEmpty(dtoList)) {
            return;
        }
        for (SubscriptionDTO dto : dtoList) {
            update(dto);
        }
    }

    @Override
    public boolean isSubscribed(Long userId, String msgType, String channelType) {
        MsgSubscription subscription = getOne(QueryWrapper.create()
                .where(MSG_SUBSCRIPTION.USER_ID.eq(userId))
                .and(MSG_SUBSCRIPTION.MSG_TYPE.eq(msgType))
                .and(MSG_SUBSCRIPTION.CHANNEL_TYPE.eq(channelType)));

        // 如果没有订阅记录，默认订阅
        return subscription == null || subscription.getSubscribed() == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultSubscriptions(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        for (String msgType : DEFAULT_MSG_TYPES) {
            MsgSubscription subscription = MsgSubscription.builder()
                    .userId(userId)
                    .msgType(msgType)
                    .channelType(DEFAULT_CHANNEL)
                    .subscribed(1)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            save(subscription);
        }

        log.info("初始化用户默认订阅: userId={}", userId);
    }
}