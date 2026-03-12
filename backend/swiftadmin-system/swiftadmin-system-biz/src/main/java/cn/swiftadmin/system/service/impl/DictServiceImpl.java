package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.enums.CommonStatus;
import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.system.api.dto.DictDataQueryDTO;
import cn.swiftadmin.system.api.dto.DictTypeQueryDTO;
import cn.swiftadmin.system.api.entity.SysDictData;
import cn.swiftadmin.system.api.entity.SysDictType;
import cn.swiftadmin.system.api.service.DictService;
import cn.swiftadmin.system.api.vo.DictDataVO;
import cn.swiftadmin.system.api.vo.DictTypeVO;
import cn.swiftadmin.system.mapper.SysDictDataMapper;
import cn.swiftadmin.system.mapper.SysDictTypeMapper;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysDictDataTableDef.SYS_DICT_DATA;
import static cn.swiftadmin.system.api.entity.table.SysDictTypeTableDef.SYS_DICT_TYPE;

/**
 * 字典服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
public class DictServiceImpl implements DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final Map<String, List<DictDataVO>> dictCache = new ConcurrentHashMap<>();

    public DictServiceImpl(SysDictTypeMapper dictTypeMapper, SysDictDataMapper dictDataMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
    }

    // ==================== 字典类型管理 ====================

    @Override
    public Page<SysDictType> pageDictType(DictTypeQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_DICT_TYPE.DICT_NAME.like(PageHelper.likeIfPresent(query.getDictName())))
                .and(SYS_DICT_TYPE.DICT_TYPE.like(PageHelper.likeIfPresent(query.getDictType())))
                .and(SYS_DICT_TYPE.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .orderBy(SYS_DICT_TYPE.CREATE_TIME.desc());

        return dictTypeMapper.paginate(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper);
    }

    @Override
    public SysDictType getDictTypeById(Long id) {
        return dictTypeMapper.selectOneById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDictType(DictTypeVO vo) {
        SysDictType dictType = BeanCopyUtils.copy(vo, SysDictType::new);
        dictTypeMapper.insert(dictType);
        clearCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictType(DictTypeVO vo) {
        SysDictType dictType = BeanCopyUtils.copy(vo, SysDictType::new);
        dictTypeMapper.update(dictType);
        clearCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictType(List<Long> ids) {
        dictTypeMapper.deleteBatchByIds(ids);
        clearCache();
    }

    // ==================== 字典数据管理 ====================

    @Override
    public Page<SysDictData> pageDictData(DictDataQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_DICT_DATA.DICT_TYPE.eq(query.getDictType(), query.getDictType() != null && !query.getDictType().isEmpty()))
                .and(SYS_DICT_DATA.LABEL.like(PageHelper.likeIfPresent(query.getDictLabel())))
                .and(SYS_DICT_DATA.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .orderBy(SYS_DICT_DATA.SORT_ORDER.asc());

        return dictDataMapper.paginate(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper);
    }

    @Override
    public SysDictData getDictDataById(Long id) {
        return dictDataMapper.selectOneById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDictData(DictDataVO vo) {
        SysDictData dictData = BeanCopyUtils.copy(vo, SysDictData::new);
        dictDataMapper.insert(dictData);
        clearCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictData(DictDataVO vo) {
        SysDictData dictData = BeanCopyUtils.copy(vo, SysDictData::new);
        dictDataMapper.update(dictData);
        clearCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictData(List<Long> ids) {
        dictDataMapper.deleteBatchByIds(ids);
        clearCache();
    }

    // ==================== 字典缓存管理 ====================

    @Override
    public List<DictDataVO> getDictDataByType(String dictType) {
        return dictCache.computeIfAbsent(dictType, this::loadDictDataByType);
    }

    @Override
    public void refreshCache() {
        dictCache.clear();

        List<SysDictData> allDictData = dictDataMapper.selectAll();
        Map<String, List<DictDataVO>> groupedDict = allDictData.stream()
                .filter(d -> d.getStatus() != null && d.getStatus().isEnabled())
                .collect(Collectors.groupingBy(
                        SysDictData::getDictType,
                        Collectors.mapping(this::toDictDataVO, Collectors.toList())
                ));

        dictCache.putAll(groupedDict);
        log.info("字典缓存刷新完成，共加载 {} 种字典类型", dictCache.size());
    }

    @Override
    public void clearCache() {
        dictCache.clear();
        log.info("字典缓存已清空");
    }

    private List<DictDataVO> loadDictDataByType(String dictType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_DICT_DATA.DICT_TYPE.eq(dictType))
                .and(SYS_DICT_DATA.STATUS.eq(CommonStatus.ENABLED))
                .orderBy(SYS_DICT_DATA.SORT_ORDER.asc());

        return dictDataMapper.selectListByQuery(queryWrapper).stream()
                .map(this::toDictDataVO)
                .collect(Collectors.toList());
    }

    private DictDataVO toDictDataVO(SysDictData dictData) {
        return BeanCopyUtils.copy(dictData, DictDataVO::new);
    }

}