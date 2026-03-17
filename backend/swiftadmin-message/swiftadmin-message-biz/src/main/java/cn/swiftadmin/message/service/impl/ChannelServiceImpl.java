package cn.swiftadmin.message.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.message.api.channel.ChannelSender;
import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.dto.ChannelDTO;
import cn.swiftadmin.message.api.dto.ChannelQueryDTO;
import cn.swiftadmin.message.api.entity.MsgChannel;
import cn.swiftadmin.message.api.enums.ChannelType;
import cn.swiftadmin.message.api.service.ChannelService;
import cn.swiftadmin.message.api.vo.ChannelVO;
import cn.swiftadmin.message.channel.ChannelSenderFactory;
import cn.swiftadmin.message.mapper.MsgChannelMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

import static cn.swiftadmin.message.api.entity.table.MsgChannelTableDef.MSG_CHANNEL;

/**
 * 消息渠道服务实现
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelServiceImpl extends ServiceImpl<MsgChannelMapper, MsgChannel> implements ChannelService {

    private final ChannelSenderFactory senderFactory;

    @Override
    public Page<ChannelVO> page(ChannelQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(MSG_CHANNEL.CHANNEL_NAME.like(PageHelper.likeIfPresent(query.getChannelName())))
                .and(MSG_CHANNEL.CHANNEL_TYPE.eq(query.getChannelType(), query.getChannelType() != null))
                .and(MSG_CHANNEL.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .orderBy(MSG_CHANNEL.PRIORITY.desc(), MSG_CHANNEL.ID.desc());

        return mapper.paginateAs(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper, ChannelVO.class);
    }

    @Override
    public ChannelVO getDetail(Long id) {
        MsgChannel channel = getById(id);
        return channel != null ? BeanCopyUtils.copy(channel, ChannelVO::new) : null;
    }

    @Override
    public MsgChannel getEntityById(Long id) {
        return getById(id);
    }

    @Override
    public MsgChannel getByCode(String channelCode) {
        return getOne(QueryWrapper.create()
                .where(MSG_CHANNEL.CHANNEL_CODE.eq(channelCode)));
    }

    @Override
    public List<MsgChannel> listEnabledByType(String channelType) {
        return list(QueryWrapper.create()
                .where(MSG_CHANNEL.CHANNEL_TYPE.eq(channelType))
                .and(MSG_CHANNEL.STATUS.eq(1))
                .orderBy(MSG_CHANNEL.PRIORITY.desc()));
    }

    @Override
    public List<MsgChannel> listAllEnabled() {
        return list(QueryWrapper.create()
                .where(MSG_CHANNEL.STATUS.eq(1))
                .orderBy(MSG_CHANNEL.PRIORITY.desc()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ChannelDTO dto) {
        if (existsByCode(dto.getChannelCode(), null)) {
            throw new BusinessException(400, "渠道编码已存在");
        }

        // 验证渠道类型
        ChannelType channelType = ChannelType.fromCode(dto.getChannelType());
        if (channelType == null) {
            throw new BusinessException(400, "无效的渠道类型: " + dto.getChannelType());
        }

        MsgChannel channel = MsgChannel.builder()
                .channelName(dto.getChannelName())
                .channelType(dto.getChannelType())
                .channelCode(dto.getChannelCode())
                .config(dto.getConfig())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .priority(dto.getPriority() != null ? dto.getPriority() : 0)
                .remark(dto.getRemark())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();

        save(channel);
        log.info("渠道创建成功: id={}, code={}", channel.getId(), channel.getChannelCode());
        return channel.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ChannelDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "渠道ID不能为空");
        }

        MsgChannel existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "渠道不存在");
        }

        // 如果修改了渠道编码，检查新编码是否已存在
        if (!existing.getChannelCode().equals(dto.getChannelCode()) && existsByCode(dto.getChannelCode(), dto.getId())) {
            throw new BusinessException(400, "渠道编码已存在");
        }

        existing.setChannelName(dto.getChannelName());
        existing.setChannelType(dto.getChannelType());
        existing.setChannelCode(dto.getChannelCode());
        existing.setConfig(dto.getConfig());
        existing.setStatus(dto.getStatus());
        existing.setPriority(dto.getPriority());
        existing.setRemark(dto.getRemark());
        existing.setUpdateTime(LocalDateTime.now());

        updateById(existing);
        log.info("渠道更新成功: id={}", existing.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // MyBatis-Flex 会自动处理逻辑删除（因为 @Column(isLogicDelete = true)）
        removeByIds(ids);
        log.info("渠道删除成功: ids={}", ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        MsgChannel channel = getById(id);
        if (channel == null) {
            throw new BusinessException(404, "渠道不存在");
        }

        channel.setStatus(status);
        channel.setUpdateTime(LocalDateTime.now());
        updateById(channel);
        log.info("渠道状态更新: id={}, status={}", id, status);
    }

    @Override
    public boolean testConnection(Long id) {
        MsgChannel channel = getById(id);
        if (channel == null) {
            throw new BusinessException(404, "渠道不存在");
        }

        ChannelType channelType = ChannelType.fromCode(channel.getChannelType());
        if (channelType == null) {
            return false;
        }

        ChannelSender sender = senderFactory.getSender(channelType);
        return sender.healthCheck(channel.getConfig());
    }

    @Override
    public boolean existsByCode(String channelCode, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(MSG_CHANNEL.CHANNEL_CODE.eq(channelCode));

        if (excludeId != null) {
            queryWrapper.and(MSG_CHANNEL.ID.ne(excludeId));
        }

        return mapper.selectCountByQuery(queryWrapper) > 0;
    }
}