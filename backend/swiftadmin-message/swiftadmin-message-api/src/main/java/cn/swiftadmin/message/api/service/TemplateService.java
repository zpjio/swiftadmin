package cn.swiftadmin.message.api.service;

import cn.swiftadmin.message.api.dto.TemplateDTO;
import cn.swiftadmin.message.api.dto.TemplateQueryDTO;
import cn.swiftadmin.message.api.entity.MsgTemplate;
import cn.swiftadmin.message.api.vo.TemplateVO;
import com.mybatisflex.core.paginate.Page;

import java.util.List;

/**
 * 消息模板服务接口
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface TemplateService {

    /**
     * 分页查询模板
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<TemplateVO> page(TemplateQueryDTO query);

    /**
     * 获取模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    TemplateVO getDetail(Long id);

    /**
     * 根据模板编码获取模板
     *
     * @param templateCode 模板编码
     * @return 模板实体
     */
    MsgTemplate getByCode(String templateCode);

    /**
     * 获取指定渠道类型的模板列表
     *
     * @param channelType 渠道类型
     * @return 模板列表
     */
    List<TemplateVO> listByChannelType(String channelType);

    /**
     * 创建模板
     *
     * @param dto 模板信息
     * @return 模板ID
     */
    Long create(TemplateDTO dto);

    /**
     * 更新模板
     *
     * @param dto 模板信息
     */
    void update(TemplateDTO dto);

    /**
     * 删除模板
     *
     * @param ids 模板ID列表
     */
    void delete(List<Long> ids);

    /**
     * 更新模板状态
     *
     * @param id 模板ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 检查模板编码是否存在
     *
     * @param templateCode 模板编码
     * @param excludeId 排除的ID
     * @return true=存在
     */
    boolean existsByCode(String templateCode, Long excludeId);

    /**
     * 渲染模板内容
     *
     * @param template 模板
     * @param variables 变量
     * @return 渲染后的内容
     */
    String renderTemplate(MsgTemplate template, java.util.Map<String, Object> variables);
}