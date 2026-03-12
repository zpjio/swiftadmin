package cn.swiftadmin.system.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.dto.ConfigDTO;
import cn.swiftadmin.system.api.dto.ConfigQueryDTO;
import cn.swiftadmin.system.api.service.ConfigService;
import cn.swiftadmin.system.api.vo.ConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置控制器
 * <p>提供系统参数的配置管理功能</p>
 * <p>用于管理系统运行时的各种配置参数，支持动态刷新</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "系统配置管理", description = "系统参数配置管理接口")
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /**
     * 分页查询配置列表
     * <p>支持按配置类型、名称等条件筛选</p>
     *
     * @param query 查询条件
     * @return 配置分页列表
     */
    @Operation(summary = "分页查询配置", description = "支持按配置类型、名称等条件筛选")
    @GetMapping("/page")
    @RequiresPermission("system:config:list")
    public R<Page<ConfigVO>> page(ConfigQueryDTO query) {
        return R.ok(configService.page(query));
    }

    /**
     * 获取配置详情
     * <p>根据配置 ID 获取完整信息</p>
     *
     * @param id 配置 ID
     * @return 配置详情
     */
    @Operation(summary = "获取配置详情", description = "根据配置 ID 获取完整信息")
    @GetMapping("/{id}")
    @RequiresPermission("system:config:query")
    public R<ConfigVO> getDetail(@PathVariable Long id) {
        return R.ok(configService.getDetail(id));
    }

    /**
     * 根据配置键获取配置
     * <p>配置键是唯一的标识符，如：system.name</p>
     *
     * @param configKey 配置键
     * @return 配置信息
     */
    @Operation(summary = "根据配置键获取配置", description = "配置键是唯一的标识符，如：system.name")
    @GetMapping("/key/{configKey}")
    @RequiresPermission("system:config:query")
    public R<ConfigVO> getByKey(@PathVariable String configKey) {
        return R.ok(configService.getByKey(configKey));
    }

    /**
     * 获取指定类型的配置列表
     * <p>按配置类型分组，如：system、email、sms 等</p>
     *
     * @param configType 配置类型
     * @return 配置列表
     */
    @Operation(summary = "获取指定类型的配置列表", description = "按配置类型分组，如：system、email、sms 等")
    @GetMapping("/type/{configType}")
    @RequiresPermission("system:config:list")
    public R<List<ConfigVO>> listByType(@PathVariable String configType) {
        return R.ok(configService.listByType(configType));
    }

    /**
     * 获取配置值（公开接口）
     * <p>无需权限即可访问，用于前端直接获取配置值</p>
     *
     * @param configKey 配置键
     * @return 配置值
     */
    @Operation(summary = "获取配置值", description = "无需权限即可访问，用于前端直接获取配置值")
    @GetMapping("/value/{configKey}")
    public R<String> getValue(@PathVariable String configKey) {
        return R.ok(configService.getValue(configKey, ""));
    }

    /**
     * 创建配置
     * <p>新增系统配置参数</p>
     *
     * @param dto 配置创建参数
     * @return 新创建的配置 ID
     */
    @Operation(summary = "创建配置", description = "新增系统配置参数")
    @PostMapping
    @RequiresPermission("system:config:add")
    @OperationLog(title = "系统配置", businessType = OperationLog.BusinessType.INSERT)
    public R<Long> create(@Valid @RequestBody ConfigDTO dto) {
        return R.ok(configService.create(dto));
    }

    /**
     * 更新配置
     * <p>修改配置值，会自动刷新缓存</p>
     *
     * @param dto 配置更新参数
     * @return 操作结果
     */
    @Operation(summary = "更新配置", description = "修改配置值，会自动刷新缓存")
    @PutMapping
    @RequiresPermission("system:config:edit")
    @OperationLog(title = "系统配置", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> update(@Valid @RequestBody ConfigDTO dto) {
        configService.update(dto);
        return R.ok();
    }

    /**
     * 批量删除配置
     * <p>支持同时删除多个配置项</p>
     *
     * @param ids 配置 ID 列表
     * @return 操作结果
     */
    @Operation(summary = "批量删除配置", description = "支持同时删除多个配置项")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:config:delete")
    @OperationLog(title = "系统配置", businessType = OperationLog.BusinessType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        configService.delete(ids);
        return R.ok();
    }

    /**
     * 刷新配置缓存
     * <p>当配置变更时，手动刷新缓存使其生效</p>
     *
     * @param configKey 配置键（可选，不传则刷新所有配置）
     * @return 操作结果
     */
    @Operation(summary = "刷新配置缓存", description = "当配置变更时，手动刷新缓存使其生效")
    @PostMapping("/cache/refresh")
    @RequiresPermission("system:config:edit")
    @OperationLog(title = "系统配置", businessType = OperationLog.BusinessType.OTHER)
    public R<Void> refreshCache(@RequestParam(required = false) String configKey) {
        configService.refreshCache(configKey);
        return R.ok();
    }

}