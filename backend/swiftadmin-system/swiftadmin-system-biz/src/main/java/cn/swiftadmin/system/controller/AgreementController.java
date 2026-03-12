package cn.swiftadmin.system.controller;

import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.dto.AgreementDTO;
import cn.swiftadmin.system.api.service.AgreementService;
import cn.swiftadmin.system.api.vo.AgreementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户协议控制器
 * <p>提供用户协议和隐私政策的管理功能</p>
 * <p>支持版本管理，可查看历史版本</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "用户协议管理", description = "用户协议和隐私政策管理接口")
@RestController
@RequestMapping("/api/system/agreement")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    /**
     * 获取用户协议
     * <p>返回当前生效的用户协议内容</p>
     *
     * @return 用户协议详情
     */
    @Operation(summary = "获取用户协议", description = "返回当前生效的用户协议内容")
    @GetMapping("/user")
    public R<AgreementVO> getUserAgreement() {
        return R.ok(agreementService.getUserAgreement());
    }

    /**
     * 获取隐私政策
     * <p>返回当前生效的隐私政策内容</p>
     *
     * @return 隐私政策详情
     */
    @Operation(summary = "获取隐私政策", description = "返回当前生效的隐私政策内容")
    @GetMapping("/privacy")
    public R<AgreementVO> getPrivacyPolicy() {
        return R.ok(agreementService.getPrivacyPolicy());
    }

    /**
     * 获取协议详情
     * <p>根据 ID 获取协议的完整信息</p>
     *
     * @param id 协议 ID
     * @return 协议详情
     */
    @Operation(summary = "获取协议详情", description = "根据 ID 获取协议的完整信息")
    @GetMapping("/{id}")
    @RequiresPermission("system:agreement:query")
    public R<AgreementVO> getDetail(@PathVariable Long id) {
        return R.ok(agreementService.getDetail(id));
    }

    /**
     * 获取协议版本列表
     * <p>返回指定类型协议的所有历史版本</p>
     *
     * @param type 协议类型（user=用户协议，privacy=隐私政策）
     * @return 协议版本列表
     */
    @Operation(summary = "获取协议版本列表", description = "返回指定类型协议的所有历史版本")
    @GetMapping("/versions")
    @RequiresPermission("system:agreement:list")
    public R<List<AgreementVO>> listVersions(@RequestParam String type) {
        return R.ok(agreementService.listVersions(type));
    }

    /**
     * 创建协议
     * <p>新增协议版本，会自动设置为生效状态</p>
     *
     * @param dto 协议创建参数
     * @return 新创建的协议 ID
     */
    @Operation(summary = "创建协议", description = "新增协议版本，会自动设置为生效状态")
    @PostMapping
    @RequiresPermission("system:agreement:add")
    @OperationLog(title = "用户协议", businessType = OperationLog.BusinessType.INSERT)
    public R<Long> create(@Valid @RequestBody AgreementDTO dto) {
        return R.ok(agreementService.create(dto));
    }

    /**
     * 更新协议
     * <p>修改协议内容，不影响已生效的版本</p>
     *
     * @param dto 协议更新参数
     * @return 操作结果
     */
    @Operation(summary = "更新协议", description = "修改协议内容，不影响已生效的版本")
    @PutMapping
    @RequiresPermission("system:agreement:edit")
    @OperationLog(title = "用户协议", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> update(@Valid @RequestBody AgreementDTO dto) {
        agreementService.update(dto);
        return R.ok();
    }

    /**
     * 批量删除协议
     * <p>支持同时删除多个协议版本</p>
     *
     * @param ids 协议 ID 列表
     * @return 操作结果
     */
    @Operation(summary = "批量删除协议", description = "支持同时删除多个协议版本")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:agreement:delete")
    @OperationLog(title = "用户协议", businessType = OperationLog.BusinessType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        agreementService.delete(ids);
        return R.ok();
    }

    /**
     * 更新协议状态
     * <p>设置协议的生效/失效状态</p>
     *
     * @param id 协议 ID
     * @param status 状态（0=失效，1=生效）
     * @return 操作结果
     */
    @PutMapping("/status")
    @RequiresPermission("system:agreement:edit")
    @OperationLog(title = "用户协议", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        agreementService.updateStatus(id, status);
        return R.ok();
    }

}