package cn.swiftadmin.message.controller;

import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.message.api.dto.SubscriptionDTO;
import cn.swiftadmin.message.api.service.SubscriptionService;
import cn.swiftadmin.message.api.vo.SubscriptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息订阅控制器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Tag(name = "消息订阅", description = "消息订阅管理接口")
@RestController
@RequestMapping("/api/message/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 获取当前用户订阅列表
     */
    @Operation(summary = "获取订阅列表", description = "获取当前用户的消息订阅列表")
    @GetMapping("/list")
    public R<List<SubscriptionVO>> list() {
        return R.ok(subscriptionService.listSubscriptions());
    }

    /**
     * 更新订阅状态
     */
    @Operation(summary = "更新订阅状态", description = "更新指定消息类型的订阅状态")
    @PutMapping
    public R<Void> update(@Valid @RequestBody SubscriptionDTO dto) {
        subscriptionService.update(dto);
        return R.ok();
    }

    /**
     * 批量更新订阅状态
     */
    @Operation(summary = "批量更新订阅", description = "批量更新消息订阅状态")
    @PutMapping("/batch")
    public R<Void> batchUpdate(@RequestBody List<SubscriptionDTO> dtoList) {
        subscriptionService.batchUpdate(dtoList);
        return R.ok();
    }
}