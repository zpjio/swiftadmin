package cn.swiftadmin.message.controller;

import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.message.api.dto.ChannelDTO;
import cn.swiftadmin.message.api.dto.ChannelQueryDTO;
import cn.swiftadmin.message.api.service.ChannelService;
import cn.swiftadmin.message.api.vo.ChannelVO;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息渠道控制器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Tag(name = "消息渠道", description = "消息渠道管理接口")
@RestController
@RequestMapping("/api/message/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    /**
     * 分页查询渠道
     */
    @Operation(summary = "分页查询渠道", description = "分页查询消息渠道列表")
    @GetMapping("/page")
    @RequiresPermission("message:channel:list")
    public R<Page<ChannelVO>> page(ChannelQueryDTO query) {
        return R.ok(channelService.page(query));
    }

    /**
     * 获取渠道详情
     */
    @Operation(summary = "获取渠道详情", description = "根据ID获取渠道详情")
    @GetMapping("/{id}")
    @RequiresPermission("message:channel:query")
    public R<ChannelVO> getDetail(@PathVariable Long id) {
        return R.ok(channelService.getDetail(id));
    }

    /**
     * 创建渠道
     */
    @Operation(summary = "创建渠道", description = "创建消息渠道")
    @PostMapping
    @RequiresPermission("message:channel:add")
    @OperationLog(title = "消息渠道", businessType = OperationLog.BusinessType.INSERT)
    public R<Long> create(@Valid @RequestBody ChannelDTO dto) {
        return R.ok(channelService.create(dto));
    }

    /**
     * 更新渠道
     */
    @Operation(summary = "更新渠道", description = "更新消息渠道")
    @PutMapping
    @RequiresPermission("message:channel:edit")
    @OperationLog(title = "消息渠道", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> update(@Valid @RequestBody ChannelDTO dto) {
        channelService.update(dto);
        return R.ok();
    }

    /**
     * 删除渠道
     */
    @Operation(summary = "删除渠道", description = "删除消息渠道")
    @DeleteMapping("/{ids}")
    @RequiresPermission("message:channel:delete")
    @OperationLog(title = "消息渠道", businessType = OperationLog.BusinessType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        channelService.delete(ids);
        return R.ok();
    }

    /**
     * 更新渠道状态
     */
    @Operation(summary = "更新渠道状态", description = "启用/禁用渠道")
    @PutMapping("/{id}/status/{status}")
    @RequiresPermission("message:channel:edit")
    @OperationLog(title = "消息渠道", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        channelService.updateStatus(id, status);
        return R.ok();
    }

    /**
     * 测试渠道连接
     */
    @Operation(summary = "测试渠道连接", description = "测试渠道是否可用")
    @PostMapping("/test/{id}")
    @RequiresPermission("message:channel:test")
    public R<Boolean> testConnection(@PathVariable Long id) {
        return R.ok(channelService.testConnection(id));
    }
}