package cn.swiftadmin.message.controller;

import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.message.api.dto.MessageDTO;
import cn.swiftadmin.message.api.dto.RecordQueryDTO;
import cn.swiftadmin.message.api.service.MessageService;
import cn.swiftadmin.message.api.vo.RecordVO;
import cn.swiftadmin.message.api.vo.SendResultVO;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息发送控制器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Tag(name = "消息发送", description = "消息发送管理接口")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 发送消息
     */
    @Operation(summary = "发送消息", description = "发送消息到指定渠道")
    @PostMapping("/send")
    @RequiresPermission("message:send")
    public R<List<SendResultVO>> send(@Valid @RequestBody MessageDTO dto) {
        return R.ok(messageService.send(dto));
    }

    /**
     * 批量发送消息
     */
    @Operation(summary = "批量发送消息", description = "批量发送多条消息")
    @PostMapping("/batch-send")
    @RequiresPermission("message:send")
    public R<List<SendResultVO>> batchSend(@RequestBody List<MessageDTO> messages) {
        return R.ok(messageService.batchSend(messages));
    }

    /**
     * 分页查询发送记录
     */
    @Operation(summary = "分页查询发送记录", description = "查询消息发送记录")
    @GetMapping("/record/page")
    @RequiresPermission("message:record:list")
    public R<Page<RecordVO>> pageRecord(RecordQueryDTO query) {
        return R.ok(messageService.pageRecord(query));
    }

    /**
     * 获取发送记录详情
     */
    @Operation(summary = "获取发送记录详情", description = "根据ID获取发送记录详情")
    @GetMapping("/record/{id}")
    @RequiresPermission("message:record:query")
    public R<RecordVO> getRecordDetail(@PathVariable Long id) {
        return R.ok(messageService.getRecordDetail(id));
    }

    /**
     * 重试发送
     */
    @Operation(summary = "重试发送", description = "重试发送失败的消息")
    @PostMapping("/record/retry/{id}")
    @RequiresPermission("message:record:retry")
    public R<SendResultVO> retry(@PathVariable Long id) {
        return R.ok(messageService.retry(id));
    }
}