package cn.swiftadmin.message.service.impl;

import cn.swiftadmin.common.core.exception.BusinessException;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.message.api.dto.TemplateDTO;
import cn.swiftadmin.message.api.dto.TemplateQueryDTO;
import cn.swiftadmin.message.api.entity.MsgTemplate;
import cn.swiftadmin.message.api.enums.ChannelType;
import cn.swiftadmin.message.api.service.TemplateService;
import cn.swiftadmin.message.api.vo.TemplateVO;
import cn.swiftadmin.message.mapper.MsgTemplateMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.swiftadmin.message.api.entity.table.MsgTemplateTableDef.MSG_TEMPLATE;

/**
 * 消息模板服务实现
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl extends ServiceImpl<MsgTemplateMapper, MsgTemplate> implements TemplateService {

    /** 模板变量匹配正则 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    @Override
    public Page<TemplateVO> page(TemplateQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(MSG_TEMPLATE.TEMPLATE_NAME.like(PageHelper.likeIfPresent(query.getTemplateName())))
                .and(MSG_TEMPLATE.TEMPLATE_CODE.like(PageHelper.likeIfPresent(query.getTemplateCode())))
                .and(MSG_TEMPLATE.CHANNEL_TYPE.eq(query.getChannelType(), query.getChannelType() != null))
                .and(MSG_TEMPLATE.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .orderBy(MSG_TEMPLATE.ID.desc());

        return mapper.paginateAs(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper, TemplateVO.class);
    }

    @Override
    public TemplateVO getDetail(Long id) {
        MsgTemplate template = getById(id);
        return template != null ? BeanCopyUtils.copy(template, TemplateVO::new) : null;
    }

    @Override
    public MsgTemplate getByCode(String templateCode) {
        return getOne(QueryWrapper.create()
                .where(MSG_TEMPLATE.TEMPLATE_CODE.eq(templateCode))
                .and(MSG_TEMPLATE.STATUS.eq(1)));
    }

    @Override
    public List<TemplateVO> listByChannelType(String channelType) {
        return BeanCopyUtils.copyList(
                list(QueryWrapper.create()
                        .where(MSG_TEMPLATE.CHANNEL_TYPE.eq(channelType))
                        .and(MSG_TEMPLATE.STATUS.eq(1))
                        .orderBy(MSG_TEMPLATE.ID.desc())),
                TemplateVO::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TemplateDTO dto) {
        if (existsByCode(dto.getTemplateCode(), null)) {
            throw new BusinessException(400, "模板编码已存在");
        }

        // 验证渠道类型
        ChannelType channelType = ChannelType.fromCode(dto.getChannelType());
        if (channelType == null) {
            throw new BusinessException(400, "无效的渠道类型: " + dto.getChannelType());
        }

        MsgTemplate template = MsgTemplate.builder()
                .templateName(dto.getTemplateName())
                .templateCode(dto.getTemplateCode())
                .channelType(dto.getChannelType())
                .subject(dto.getSubject())
                .content(dto.getContent())
                .variables(dto.getVariables())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .remark(dto.getRemark())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();

        save(template);
        log.info("模板创建成功: id={}, code={}", template.getId(), template.getTemplateCode());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TemplateDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "模板ID不能为空");
        }

        MsgTemplate existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(404, "模板不存在");
        }

        // 如果修改了模板编码，检查新编码是否已存在
        if (!existing.getTemplateCode().equals(dto.getTemplateCode()) && existsByCode(dto.getTemplateCode(), dto.getId())) {
            throw new BusinessException(400, "模板编码已存在");
        }

        UpdateChain.of(MsgTemplate.class)
                .set(MSG_TEMPLATE.TEMPLATE_NAME, dto.getTemplateName())
                .set(MSG_TEMPLATE.TEMPLATE_CODE, dto.getTemplateCode())
                .set(MSG_TEMPLATE.CHANNEL_TYPE, dto.getChannelType())
                .set(MSG_TEMPLATE.SUBJECT, dto.getSubject())
                .set(MSG_TEMPLATE.CONTENT, dto.getContent())
                .set(MSG_TEMPLATE.VARIABLES, dto.getVariables())
                .set(MSG_TEMPLATE.STATUS, dto.getStatus())
                .set(MSG_TEMPLATE.REMARK, dto.getRemark())
                .set(MSG_TEMPLATE.UPDATE_TIME, LocalDateTime.now())
                .where(MSG_TEMPLATE.ID.eq(dto.getId()))
                .update();

        log.info("模板更新成功: id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // MyBatis-Flex 会自动处理逻辑删除（因为 @Column(isLogicDelete = true)）
        removeByIds(ids);
        log.info("模板删除成功: ids={}", ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        UpdateChain.of(MsgTemplate.class)
                .set(MSG_TEMPLATE.STATUS, status)
                .set(MSG_TEMPLATE.UPDATE_TIME, LocalDateTime.now())
                .where(MSG_TEMPLATE.ID.eq(id))
                .update();

        log.info("模板状态更新: id={}, status={}", id, status);
    }

    @Override
    public boolean existsByCode(String templateCode, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(MSG_TEMPLATE.TEMPLATE_CODE.eq(templateCode));

        if (excludeId != null) {
            queryWrapper.and(MSG_TEMPLATE.ID.ne(excludeId));
        }

        return mapper.selectCountByQuery(queryWrapper) > 0;
    }

    @Override
    public String renderTemplate(MsgTemplate template, Map<String, Object> variables) {
        if (template == null || !StringUtils.hasText(template.getContent())) {
            return "";
        }

        String content = template.getContent();
        if (variables == null || variables.isEmpty()) {
            return content;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}