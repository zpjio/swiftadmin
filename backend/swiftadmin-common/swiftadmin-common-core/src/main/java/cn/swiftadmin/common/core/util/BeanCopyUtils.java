package cn.swiftadmin.common.core.util;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Bean 拷贝工具类
 * <p>提供高效的 Bean 属性拷贝功能，支持单个对象和列表拷贝</p>
 * <p>基于 Spring BeanUtils 实现，性能优化且类型安全</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public final class BeanCopyUtils {

    private BeanCopyUtils() {
    }

    /**
     * 单个对象拷贝
     *
     * @param source 源对象
     * @param targetSupplier 目标对象构造器
     * @return 拷贝后的目标对象
     */
    public static <S, T> T copy(S source, Supplier<T> targetSupplier) {
        return copy(source, targetSupplier, (String[]) null);
    }

    /**
     * 单个对象拷贝（指定忽略属性）
     *
     * @param source 源对象
     * @param targetSupplier 目标对象构造器
     * @param ignoreProperties 需要忽略的属性数组
     * @return 拷贝后的目标对象
     */
    public static <S, T> T copy(S source, Supplier<T> targetSupplier, String... ignoreProperties) {
        if (Objects.isNull(source) || Objects.isNull(targetSupplier)) {
            return null;
        }
        T target = targetSupplier.get();
        BeanUtils.copyProperties(source, target, ignoreProperties);
        return target;
    }

    /**
     * 单个对象拷贝（带后置处理）
     * <p>适用于需要手动处理特殊字段（如枚举转换）的场景</p>
     *
     * @param source 源对象
     * @param targetSupplier 目标对象构造器
     * @param postProcessor 后置处理器，用于处理特殊字段
     * @return 拷贝后的目标对象
     */
    public static <S, T> T copy(S source, Supplier<T> targetSupplier, BiConsumer<S, T> postProcessor) {
        if (Objects.isNull(source) || Objects.isNull(targetSupplier)) {
            return null;
        }
        T target = targetSupplier.get();
        BeanUtils.copyProperties(source, target);
        if (postProcessor != null) {
            postProcessor.accept(source, target);
        }
        return target;
    }

    /**
     * 单个对象拷贝（使用函数式转换）
     *
     * @param source 源对象
     * @param converter 转换函数
     * @return 拷贝后的目标对象
     */
    public static <S, T> T copy(S source, Function<S, T> converter) {
        if (Objects.isNull(source) || Objects.isNull(converter)) {
            return null;
        }
        return converter.apply(source);
    }

    /**
     * 列表拷贝
     *
     * @param sourceList 源对象列表
     * @param targetSupplier 目标对象构造器
     * @return 拷贝后的目标对象列表
     */
    public static <S, T> List<T> copyList(List<S> sourceList, Supplier<T> targetSupplier) {
        return copyList(sourceList, targetSupplier, (String[]) null);
    }

    /**
     * 列表拷贝（指定忽略属性）
     *
     * @param sourceList 源对象列表
     * @param targetSupplier 目标对象构造器
     * @param ignoreProperties 需要忽略的属性数组
     * @return 拷贝后的目标对象列表
     */
    public static <S, T> List<T> copyList(List<S> sourceList, Supplier<T> targetSupplier, String... ignoreProperties) {
        if (Objects.isNull(sourceList) || sourceList.isEmpty() || Objects.isNull(targetSupplier)) {
            return Collections.emptyList();
        }

        if (sourceList instanceof ArrayList && sourceList.size() < 10) {
            List<T> resultList = new ArrayList<>(sourceList.size());
            for (S source : sourceList) {
                resultList.add(copy(source, targetSupplier, ignoreProperties));
            }
            return resultList;
        }

        return sourceList.stream()
                .map(source -> copy(source, targetSupplier, ignoreProperties))
                .collect(Collectors.toList());
    }

    /**
     * 列表拷贝（带后置处理）
     * <p>适用于需要手动处理特殊字段（如枚举转换）的场景</p>
     *
     * @param sourceList 源对象列表
     * @param targetSupplier 目标对象构造器
     * @param postProcessor 后置处理器
     * @return 拷贝后的目标对象列表
     */
    public static <S, T> List<T> copyList(List<S> sourceList, Supplier<T> targetSupplier, BiConsumer<S, T> postProcessor) {
        if (Objects.isNull(sourceList) || sourceList.isEmpty() || Objects.isNull(targetSupplier)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> copy(source, targetSupplier, postProcessor))
                .collect(Collectors.toList());
    }

    /**
     * 列表拷贝并映射
     *
     * @param sourceList 源对象列表
     * @param mapper 映射函数
     * @return 拷贝后的目标对象列表
     */
    public static <S, T> List<T> copyList(List<S> sourceList, Function<S, T> mapper) {
        if (Objects.isNull(sourceList) || sourceList.isEmpty() || Objects.isNull(mapper)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    /**
     * 列表拷贝（并行流处理，适合大数据量）
     *
     * @param sourceList 源对象列表
     * @param targetSupplier 目标对象构造器
     * @return 拷贝后的目标对象列表
     */
    public static <S, T> List<T> copyListParallel(List<S> sourceList, Supplier<T> targetSupplier) {
        if (Objects.isNull(sourceList) || sourceList.isEmpty() || Objects.isNull(targetSupplier)) {
            return Collections.emptyList();
        }
        return sourceList.parallelStream()
                .map(source -> copy(source, targetSupplier))
                .collect(Collectors.toList());
    }

    /**
     * 列表拷贝（并行流处理，指定忽略属性）
     *
     * @param sourceList 源对象列表
     * @param targetSupplier 目标对象构造器
     * @param ignoreProperties 需要忽略的属性数组
     * @return 拷贝后的目标对象列表
     */
    public static <S, T> List<T> copyListParallel(List<S> sourceList, Supplier<T> targetSupplier, String... ignoreProperties) {
        if (Objects.isNull(sourceList) || sourceList.isEmpty() || Objects.isNull(targetSupplier)) {
            return Collections.emptyList();
        }
        return sourceList.parallelStream()
                .map(source -> copy(source, targetSupplier, ignoreProperties))
                .collect(Collectors.toList());
    }
}