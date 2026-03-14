package cn.swiftadmin.message.controller;

import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.message.api.dto.InboxMessageDTO;
import cn.swiftadmin.message.api.dto.InboxQueryDTO;
import cn.swiftadmin.message.api.service.InboxService;
import cn.swiftadmin.message.api.vo.InboxVO;
import cn.swiftadmin.message.api.vo.UnreadCountVO;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内信控制器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Tag(name = "站内信", description = "站内信管理接口")
@RestController
@RequestMapping("/api/message/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    /**
     * 分页查询站内信
     */
    @Operation(summary = "分页查询站内信", description = "分页查询当前用户的站内信")
    @GetMapping("/page")
    @RequiresPermission("message:inbox:list")
    public R<Page<InboxVO>> page(InboxQueryDTO query) {
        return R.ok(inboxService.page(query));
    }

    /**
     * 获取站内信详情
     */
    @Operation(summary = "获取站内信详情", description = "根据ID获取站内信详情")
    @GetMapping("/{id}")
    @RequiresPermission("message:inbox:query")
    public R<InboxVO> getDetail(@PathVariable Long id) {
        return R.ok(inboxService.getDetail(id));
    }

    /**
     * 获取未读消息数量
     */
    @Operation(summary = "获取未读消息数量", description = "获取当前用户未读消息数量")
    @GetMapping("/unread-count")
    public R<UnreadCountVO> getUnreadCount() {
        return R.ok(inboxService.getUnreadCount());
    }

    /**
     * 发送站内信
     */
    @Operation(summary = "发送站内信", description = "发送站内信给指定用户")
    @PostMapping("/send")
    @RequiresPermission("message:inbox:send")
    @OperationLog(title = "站内信", businessType = OperationLog.BusinessType.INSERT)
    public R<Integer> send(@Valid @RequestBody InboxMessageDTO dto) {
        return R.ok(inboxService.send(dto));
    }

    /**
     * 标记消息已读
     */
    @Operation(summary = "标记消息已读", description = "标记指定消息为已读")
    @PutMapping("/read")
    @RequiresPermission("message:inbox:read")
    public R<Void> markRead(@RequestBody List<Long> ids) {
        inboxService.markRead(ids);
        return R.ok();
    }

    /**
     * 标记全部已读
     */
    @Operation(summary = "标记全部已读", description = "标记当前用户所有消息为已读")
    @PutMapping("/read-all")
    @RequiresPermission("message:inbox:read")
    public R<Void> markAllRead() {
        inboxService.markAllRead();
        return R.ok();
    }

    /**
     * 设置星标
     */
    @Operation(summary = "设置星标", description = "设置或取消消息星标")
    @PutMapping("/{id}/star/{isStar}")
    @RequiresPermission("message:inbox:query")
    public R<Void> setStar(@PathVariable Long id, @PathVariable boolean isStar) {
        inboxService.setStar(id, isStar);
        return R.ok();
    }

    /**
     * 删除站内信
     */
    @Operation(summary = "删除站内信", description = "删除指定站内信")
    @DeleteMapping("/{ids}")
    @RequiresPermission("message:inbox:delete")
    @OperationLog(title = "站内信", businessType = OperationLog.BusinessType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        inboxService.delete(ids);
        return R.ok();
    }
}