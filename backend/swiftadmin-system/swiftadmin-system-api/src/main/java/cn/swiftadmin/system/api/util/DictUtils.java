package cn.swiftadmin.system.api.util;

import cn.swiftadmin.system.api.service.DictService;
import cn.swiftadmin.system.api.vo.DictDataVO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 字典工具类
 * <p>提供便捷的字典翻译功能</p>
 * <p>使用二级 Map 缓存实现 O(1) 查找</p>
 *
 * @author zhangpengju
 * @since 2026-03-11
 */
public final class DictUtils {

    private DictUtils() {
    }

    /** 字典服务实例（由 Spring 容器注入） */
    private static DictService dictService;

    /** 字典列表缓存（用于避免重复查询） */
    private static final Map<String, List<DictDataVO>> LOCAL_CACHE = new ConcurrentHashMap<>();

    /** value -> label 缓存：dictType -> (value -> label) */
    private static final Map<String, Map<String, String>> VALUE_TO_LABEL_CACHE = new ConcurrentHashMap<>();

    /** label -> value 缓存：dictType -> (label -> value) */
    private static final Map<String, Map<String, String>> LABEL_TO_VALUE_CACHE = new ConcurrentHashMap<>();

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
        return LOCAL_CACHE.computeIfAbsent(dictType, k -> {
            List<DictDataVO> data = dictService.getDictDataByType(dictType);
            // 同时构建索引缓存
            buildIndexCache(dictType, data);
            return data;
        });
    }

    /**
     * 构建索引缓存
     *
     * @param dictType 字典类型
     * @param dataList 字典数据列表
     */
    private static void buildIndexCache(String dictType, List<DictDataVO> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        Map<String, String> valueToLabel = new ConcurrentHashMap<>();
        Map<String, String> labelToValue = new ConcurrentHashMap<>();

        for (DictDataVO data : dataList) {
            if (data.getValue() != null && data.getLabel() != null) {
                valueToLabel.put(data.getValue(), data.getLabel());
                labelToValue.put(data.getLabel(), data.getValue());
            }
        }

        VALUE_TO_LABEL_CACHE.put(dictType, valueToLabel);
        LABEL_TO_VALUE_CACHE.put(dictType, labelToValue);
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

        // 确保缓存已加载
        getDictData(dictType);

        // 使用索引缓存 O(1) 查找
        Map<String, String> valueToLabel = VALUE_TO_LABEL_CACHE.get(dictType);
        if (valueToLabel != null) {
            return valueToLabel.getOrDefault(dictValue, defaultValue);
        }

        return defaultValue;
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

        // 确保缓存已加载
        getDictData(dictType);

        // 使用索引缓存 O(1) 查找
        Map<String, String> labelToValue = LABEL_TO_VALUE_CACHE.get(dictType);
        if (labelToValue != null) {
            return labelToValue.getOrDefault(dictLabel, defaultValue);
        }

        return defaultValue;
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

        // 确保缓存已加载
        getDictData(dictType);

        // 使用索引缓存 O(1) 查找
        Map<String, String> valueToLabel = VALUE_TO_LABEL_CACHE.get(dictType);
        return valueToLabel != null && valueToLabel.containsKey(dictValue);
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
        VALUE_TO_LABEL_CACHE.clear();
        LABEL_TO_VALUE_CACHE.clear();
        if (dictService != null) {
            dictService.refreshCache();
        }
    }

    /**
     * 清除本地缓存
     */
    public static void clearCache() {
        LOCAL_CACHE.clear();
        VALUE_TO_LABEL_CACHE.clear();
        LABEL_TO_VALUE_CACHE.clear();
    }

    /**
     * 清除指定字典类型的缓存
     *
     * @param dictType 字典类型
     */
    public static void clearCache(String dictType) {
        LOCAL_CACHE.remove(dictType);
        VALUE_TO_LABEL_CACHE.remove(dictType);
        LABEL_TO_VALUE_CACHE.remove(dictType);
    }

}