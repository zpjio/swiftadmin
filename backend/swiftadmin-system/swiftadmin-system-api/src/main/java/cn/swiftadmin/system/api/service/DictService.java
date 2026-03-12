package cn.swiftadmin.system.api.service;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.system.api.dto.DictDataQueryDTO;
import cn.swiftadmin.system.api.dto.DictTypeQueryDTO;
import cn.swiftadmin.system.api.entity.SysDictData;
import cn.swiftadmin.system.api.entity.SysDictType;
import cn.swiftadmin.system.api.vo.DictDataVO;
import cn.swiftadmin.system.api.vo.DictTypeVO;

import java.util.List;

/**
 * 字典服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface DictService {

    // ==================== 字典类型管理 ====================

    /**
     * 分页查询字典类型
     */
    Page<SysDictType> pageDictType(DictTypeQueryDTO query);

    /**
     * 根据 ID 获取字典类型
     */
    SysDictType getDictTypeById(Long id);

    /**
     * 创建字典类型
     */
    void createDictType(DictTypeVO vo);

    /**
     * 更新字典类型
     */
    void updateDictType(DictTypeVO vo);

    /**
     * 删除字典类型
     */
    void deleteDictType(List<Long> ids);

    // ==================== 字典数据管理 ====================

    /**
     * 分页查询字典数据
     */
    Page<SysDictData> pageDictData(DictDataQueryDTO query);

    /**
     * 根据 ID 获取字典数据
     */
    SysDictData getDictDataById(Long id);

    /**
     * 创建字典数据
     */
    void createDictData(DictDataVO vo);

    /**
     * 更新字典数据
     */
    void updateDictData(DictDataVO vo);

    /**
     * 删除字典数据
     */
    void deleteDictData(List<Long> ids);

    // ==================== 字典缓存管理 ====================

    /**
     * 根据字典类型获取字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    List<DictDataVO> getDictDataByType(String dictType);

    /**
     * 刷新字典缓存
     */
    void refreshCache();

    /**
     * 清除字典缓存
     */
    void clearCache();

}