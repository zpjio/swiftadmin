package cn.swiftadmin.system.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.dto.DictDataQueryDTO;
import cn.swiftadmin.system.api.dto.DictTypeQueryDTO;
import cn.swiftadmin.system.api.entity.SysDictData;
import cn.swiftadmin.system.api.entity.SysDictType;
import cn.swiftadmin.system.api.service.DictService;
import cn.swiftadmin.system.api.vo.DictDataVO;
import cn.swiftadmin.system.api.vo.DictTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理控制器
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "字典管理", description = "系统字典数据管理接口")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ==================== 字典类型管理 ====================

    @Operation(summary = "分页查询字典类型", description = "支持按字典名称、类型、状态筛选")
    @GetMapping("/dict-type/page")
    @RequiresPermission("system:dict:list")
    public R<Page<SysDictType>> pageDictType(DictTypeQueryDTO query) {
        return R.ok(dictService.pageDictType(query));
    }

    @Operation(summary = "获取字典类型详情")
    @GetMapping("/dict-type/{id}")
    @RequiresPermission("system:dict:query")
    public R<SysDictType> getDictType(@PathVariable Long id) {
        return R.ok(dictService.getDictTypeById(id));
    }

    @Operation(summary = "创建字典类型")
    @PostMapping("/dict-type")
    @RequiresPermission("system:dict:add")
    @OperationLog(title = "字典类型", businessType = OperationLog.BusinessType.CREATE)
    public R<Void> createDictType(@RequestBody DictTypeVO vo) {
        dictService.createDictType(vo);
        return R.ok();
    }

    @Operation(summary = "更新字典类型")
    @PutMapping("/dict-type")
    @RequiresPermission("system:dict:edit")
    @OperationLog(title = "字典类型", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> updateDictType(@RequestBody DictTypeVO vo) {
        dictService.updateDictType(vo);
        return R.ok();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/dict-type/{ids}")
    @RequiresPermission("system:dict:delete")
    @OperationLog(title = "字典类型", businessType = OperationLog.BusinessType.DELETE)
    public R<Void> deleteDictType(@PathVariable List<Long> ids) {
        dictService.deleteDictType(ids);
        return R.ok();
    }

    // ==================== 字典数据管理 ====================

    @Operation(summary = "分页查询字典数据", description = "支持按字典类型、标签、状态筛选")
    @GetMapping("/dict-data/page")
    @RequiresPermission("system:dict:list")
    public R<Page<SysDictData>> pageDictData(DictDataQueryDTO query) {
        return R.ok(dictService.pageDictData(query));
    }

    @Operation(summary = "获取字典数据详情")
    @GetMapping("/dict-data/{id}")
    @RequiresPermission("system:dict:query")
    public R<SysDictData> getDictData(@PathVariable Long id) {
        return R.ok(dictService.getDictDataById(id));
    }

    @Operation(summary = "创建字典数据")
    @PostMapping("/dict-data")
    @RequiresPermission("system:dict:add")
    @OperationLog(title = "字典数据", businessType = OperationLog.BusinessType.CREATE)
    public R<Void> createDictData(@RequestBody DictDataVO vo) {
        dictService.createDictData(vo);
        return R.ok();
    }

    @Operation(summary = "更新字典数据")
    @PutMapping("/dict-data")
    @RequiresPermission("system:dict:edit")
    @OperationLog(title = "字典数据", businessType = OperationLog.BusinessType.UPDATE)
    public R<Void> updateDictData(@RequestBody DictDataVO vo) {
        dictService.updateDictData(vo);
        return R.ok();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/dict-data/{ids}")
    @RequiresPermission("system:dict:delete")
    @OperationLog(title = "字典数据", businessType = OperationLog.BusinessType.DELETE)
    public R<Void> deleteDictData(@PathVariable List<Long> ids) {
        dictService.deleteDictData(ids);
        return R.ok();
    }

    // ==================== 字典缓存管理 ====================

    @Operation(summary = "获取字典数据", description = "返回指定类型的所有字典项，按排序排列")
    @GetMapping("/dict/type/{dictType}")
    public R<List<DictDataVO>> getDictDataByType(@PathVariable String dictType) {
        return R.ok(dictService.getDictDataByType(dictType));
    }

    @Operation(summary = "刷新字典缓存")
    @PostMapping("/dict/refresh")
    @RequiresPermission("system:dict:edit")
    @OperationLog(title = "字典缓存", businessType = OperationLog.BusinessType.REFRESH)
    public R<Void> refreshCache() {
        dictService.refreshCache();
        return R.ok();
    }

    @Operation(summary = "清除字典缓存")
    @DeleteMapping("/dict/cache")
    @RequiresPermission("system:dict:edit")
    @OperationLog(title = "字典缓存", businessType = OperationLog.BusinessType.CLEAN)
    public R<Void> clearCache() {
        dictService.clearCache();
        return R.ok();
    }

}