package cn.swiftadmin.system.controller;

import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.dto.DeptDTO;
import cn.swiftadmin.system.api.service.DeptService;
import cn.swiftadmin.system.api.vo.DeptVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 * <p>提供部门的增删改查功能</p>
 * <p>支持树形结构展示，用于系统组织架构管理</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "部门管理", description = "系统组织架构管理接口")
@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    /**
     * 获取部门树形列表
     * <p>返回完整的部门树形结构，支持按状态过滤</p>
     *
     * @param status 部门状态（可选，0=禁用，1=启用）
     * @return 部门树形列表
     */
    @Operation(summary = "获取部门树", description = "返回完整的部门树形结构，支持按状态过滤")
    @GetMapping("/tree")
    @RequiresPermission("system:dept:list")
    public R<List<DeptVO>> getTree(@RequestParam(required = false) Integer status) {
        return R.ok(deptService.getTree(status));
    }

    /**
     * 获取部门详情
     * <p>根据部门 ID 获取详细信息</p>
     *
     * @param id 部门 ID
     * @return 部门详情
     */
    @Operation(summary = "获取部门详情", description = "根据部门 ID 获取详细信息")
    @GetMapping("/{id}")
    @RequiresPermission("system:dept:query")
    public R<DeptVO> getDetail(@PathVariable Long id) {
        return R.ok(deptService.getDetail(id));
    }

    /**
     * 创建部门
     * <p>支持创建多级部门，需要指定父级部门 ID</p>
     *
     * @param dto 部门创建参数
     * @return 新创建的部门 ID
     */
    @Operation(summary = "创建部门", description = "支持创建多级部门，需要指定父级部门 ID")
    @PostMapping
    @RequiresPermission("system:dept:add")
    public R<Long> create(@Valid @RequestBody DeptDTO dto) {
        return R.ok(deptService.create(dto));
    }

    /**
     * 更新部门信息
     * <p>支持修改部门名称、排序、负责人等</p>
     *
     * @param dto 部门更新参数
     * @return 操作结果
     */
    @Operation(summary = "更新部门", description = "支持修改部门名称、排序、负责人等")
    @PutMapping
    @RequiresPermission("system:dept:edit")
    public R<Void> update(@Valid @RequestBody DeptDTO dto) {
        deptService.update(dto);
        return R.ok();
    }

    /**
     * 删除部门
     * <p>删除前会检查是否有子部门，有子部门时不允许删除</p>
     *
     * @param id 部门 ID
     * @return 操作结果
     */
    @Operation(summary = "删除部门", description = "删除部门，有子部门时不允许删除")
    @DeleteMapping("/{id}")
    @RequiresPermission("system:dept:delete")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }

    /**
     * 获取子部门 ID 列表
     * <p>递归获取所有下级部门的 ID，用于数据权限过滤</p>
     *
     * @param id 部门 ID
     * @return 子部门 ID 列表
     */
    @GetMapping("/{id}/children")
    @RequiresPermission("system:dept:list")
    public R<List<Long>> getChildrenIds(@PathVariable Long id) {
        return R.ok(deptService.getChildrenIds(id));
    }

}