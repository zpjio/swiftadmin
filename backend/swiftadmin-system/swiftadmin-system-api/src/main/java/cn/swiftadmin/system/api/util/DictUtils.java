package cn.swiftadmin.system.api.util;

import cn.swiftadmin.system.api.service.DictService;
import cn.swiftadmin.system.api.vo.DictDataVO;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 字典工具类
 * <p>提供便捷的字典翻译功能</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
public final class DictUtils {

    private DictUtils() {
    }

    /** 字典服务实例（由 Spring 容器注入） */
    private static DictService dictService;

    /** 字典缓存（用于避免重复查询） */
    private static final Map<String, List<DictDataVO>> LOCAL_CACHE = new ConcurrentHashMap<>();

    /**
     * 设置字典服务实例
     * <p>由 Spring 容器在启动时调用</p>
     *
     * @param service 字典服务
     */
    public static void setDictService(DictService service) {
        dictService = service;
    }

    /**
     * 根据字典类型获取字典数据列表
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    public static List<DictDataVO> getDictData(String dictType) {
        if (dictService == null) {
            return List.of();
        }
        return LOCAL_CACHE.computeIfAbsent(dictType, k -> dictService.getDictDataByType(dictType));
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @return 字典标签，未找到返回 null
     */
    public static String getLabel(String dictType, String dictValue) {
        return getLabel(dictType, dictValue, null);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType     字典类型
     * @param dictValue    字典值
     * @param defaultValue 默认值
     * @return 字典标签，未找到返回默认值
     */
    public static String getLabel(String dictType, String dictValue, String defaultValue) {
        if (dictType == null || dictValue == null) {
            return defaultValue;
        }

        List<DictDataVO> dictDataList = getDictData(dictType);
        return dictDataList.stream()
                .filter(d -> dictValue.equals(d.getValue()))
                .map(DictDataVO::getLabel)
                .findFirst()
                .orElse(defaultValue);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @return 字典值，未找到返回 null
     */
    public static String getValue(String dictType, String dictLabel) {
        return getValue(dictType, dictLabel, null);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType     字典类型
     * @param dictLabel    字典标签
     * @param defaultValue 默认值
     * @return 字典值，未找到返回默认值
     */
    public static String getValue(String dictType, String dictLabel, String defaultValue) {
        if (dictType == null || dictLabel == null) {
            return defaultValue;
        }

        List<DictDataVO> dictDataList = getDictData(dictType);
        return dictDataList.stream()
                .filter(d -> dictLabel.equals(d.getLabel()))
                .map(DictDataVO::getValue)
                .findFirst()
                .orElse(defaultValue);
    }

    /**
     * 判断字典值是否存在
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @return true=存在
     */
    public static boolean existsValue(String dictType, String dictValue) {
        if (dictType == null || dictValue == null) {
            return false;
        }

        List<DictDataVO> dictDataList = getDictData(dictType);
        return dictDataList.stream()
                .anyMatch(d -> dictValue.equals(d.getValue()));
    }

    /**
     * 翻译字典值（便捷方法）
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @return 字典标签，未找到返回原值
     */
    public static String translate(String dictType, String dictValue) {
        return getLabel(dictType, dictValue, dictValue);
    }

    /**
     * 翻译字典值（带 Supplier，用于延迟计算默认值）
     *
     * @param dictType     字典类型
     * @param dictValue    字典值
     * @param defaultValue 默认值提供者
     * @return 字典标签
     */
    public static String translate(String dictType, String dictValue, Supplier<String> defaultValue) {
        String label = getLabel(dictType, dictValue, null);
        return label != null ? label : (defaultValue != null ? defaultValue.get() : dictValue);
    }

    /**
     * 刷新本地缓存
     */
    public static void refreshCache() {
        LOCAL_CACHE.clear();
        if (dictService != null) {
            dictService.refreshCache();
        }
    }

    /**
     * 清除本地缓存
     */
    public static void clearCache() {
        LOCAL_CACHE.clear();
    }

    /**
     * 清除指定字典类型的缓存
     *
     * @param dictType 字典类型
     */
    public static void clearCache(String dictType) {
        LOCAL_CACHE.remove(dictType);
    }

}