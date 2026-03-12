package cn.swiftadmin.common.mybatis.util;

import cn.swiftadmin.common.core.result.Page;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具类
 * <p>基于 MyBatis-Flex 的分页最佳实践</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public final class PageHelper {

    private PageHelper() {
    }

    /**
     * 创建分页请求
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return MyBatis-Flex 分页对象
     */
    public static <T> com.mybatisflex.core.paginate.Page<T> of(int page, int pageSize) {
        return new com.mybatisflex.core.paginate.Page<>(page, pageSize);
    }

    /**
     * 转换 MyBatis-Flex 分页结果为业务分页结果
     *
     * @param flexPage MyBatis-Flex 分页结果
     * @param mapper   实体到 VO 的转换函数
     * @return 业务分页结果
     */
    public static <E, V> Page<V> convert(com.mybatisflex.core.paginate.Page<E> flexPage, Function<E, V> mapper) {
        List<V> records = flexPage.getRecords().stream()
                .map(mapper)
                .collect(Collectors.toList());
        return Page.of(records, flexPage.getTotalRow(), flexPage.getPageNumber(), flexPage.getPageSize());
    }

    /**
     * 转换 MyBatis-Flex 分页结果为业务分页结果（无转换）
     *
     * @param flexPage MyBatis-Flex 分页结果
     * @return 业务分页结果
     */
    public static <E> Page<E> convert(com.mybatisflex.core.paginate.Page<E> flexPage) {
        return Page.of(flexPage.getRecords(), flexPage.getTotalRow(), flexPage.getPageNumber(), flexPage.getPageSize());
    }

    /**
     * 条件拼接：模糊查询（值不为空时生效）
     * <p>返回带有前后百分号的字符串，用于 LIKE 查询</p>
     *
     * @param value 原始字符串
     * @return 格式化后的 LIKE 查询值，或 null
     */
    public static String likeIfPresent(String value) {
        if (StringUtils.hasText(value)) {
            return "%" + value.trim() + "%";
        }
        return null;
    }

    /**
     * 条件拼接：右模糊查询（值不为空时生效）
     * <p>返回带有前百分号的字符串，用于 LIKE 查询</p>
     *
     * @param value 原始字符串
     * @return 格式化后的 LIKE 查询值，或 null
     */
    public static String likeRightIfPresent(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim() + "%";
        }
        return null;
    }

    /**
     * 条件拼接：左模糊查询（值不为空时生效）
     * <p>返回带有后百分号的字符串，用于 LIKE 查询</p>
     *
     * @param value 原始字符串
     * @return 格式化后的 LIKE 查询值，或 null
     */
    public static String likeLeftIfPresent(String value) {
        if (StringUtils.hasText(value)) {
            return "%" + value.trim();
        }
        return null;
    }

    /**
     * 判断集合是否有效（非 null 且非空）
     * <p>使用 Spring CollectionUtils</p>
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !CollectionUtils.isEmpty(collection);
    }

    /**
     * 判断集合是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return CollectionUtils.isEmpty(collection);
    }

}