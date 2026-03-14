package cn.swiftadmin.message.controller;

import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.message.api.dto.TemplateDTO;
import cn.swiftadmin.message.api.dto.TemplateQueryDTO;
import cn.swiftadmin.message.api.service.TemplateService;
import cn.swiftadmin.message.api.vo.TemplateVO;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息模板控制器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Tag(name = "消息模板", description = "消息模板管理接口")
@RestController
@RequestMapping("/api/message/template")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    /**
     * 分页查询模板
     */
    @Operation(summary = "分页查询模板", description = "分页查询消息模板列表")
    @GetMapping("/page")
    @RequiresPermission("message:template:list")
    public R<Page<TemplateVO>> page(TemplateQueryDTO query) {
        return R.ok(templateService.page(query));
    }

    /**
     * 获取模板详情
     */
    @Operation(summary = "获取模板详情", description = "根据ID获取模板详情")
    @GetMapping("/{id}")
    @RequiresPermission("message:template:query")
    public R<TemplateVO> getDetail(@PathVariable Long id) {
        return R.ok(templateService.getDetail(id));
    }

    /**
     * 获取指定渠道的模板列表
     */
    @Operation(summary = "获取渠道模板列表", description = "获取指定渠道类型的模板列表")
    @GetMapping("/channel/{channelType}")
    @RequiresPermission("message:template:list")
    public R<List<TemplateVO>> listByChannelType(@PathVariable String channelType) {
        return R.ok(templateService.listByChannelType(channelType));
    }

    /**
     * 创建模板
     */
    @Operation(summary = "创建模板", description = "创建消息模板")
    @PostMapping
    @RequiresPermission("message:template:add")
    @OperationLog(title = "消息模板", businessType = OperationLog.BusinessType.INSERT)
    public R<Long> create(@Valid @RequestBody TemplateDTO dto) {
        return R.ok(templateService.create(dto));
    }

    /**
     * 更新模板
     */
    @Operation(summary = "更新模板", description = "更新消息模板")
    @PutMapping
    @RequiresPermission("message:template:edit")
    @OperationLog(title = "消息模板", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> update(@Valid @RequestBody TemplateDTO dto) {
        templateService.update(dto);
        return R.ok();
    }

    /**
     * 删除模板
     */
    @Operation(summary = "删除模板", description = "删除消息模板")
    @DeleteMapping("/{ids}")
    @RequiresPermission("message:template:delete")
    @OperationLog(title = "消息模板", businessType = OperationLog.BusinessType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        templateService.delete(ids);
        return R.ok();
    }

    /**
     * 更新模板状态
     */
    @Operation(summary = "更新模板状态", description = "启用/禁用模板")
    @PutMapping("/{id}/status/{status}")
    @RequiresPermission("message:template:edit")
    @OperationLog(title = "消息模板", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        templateService.updateStatus(id, status);
        return R.ok();
    }
}