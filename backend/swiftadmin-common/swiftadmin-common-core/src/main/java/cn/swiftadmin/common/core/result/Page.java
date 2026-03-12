package cn.swiftadmin.common.core.result;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应类
 * <p>封装分页查询结果，包含数据列表、总数、页码、页大小和总页数</p>
 *
 * @param <T> 数据类型
 * @author zhangpengju
 * @since 2026-03-09
 */
@Data
public class Page<T> {

    /** 数据列表 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long page;

    /** 每页大小 */
    private long pageSize;

    /** 总页数 */
    private long pages;

    public Page() {
        this.records = Collections.emptyList();
    }

    public Page(List<T> records, long total, long page, long pageSize) {
        this.records = records != null ? records : Collections.emptyList();
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.pages = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0;
    }

    /**
     * 构建分页结果
     *
     * @param records 数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public static <T> Page<T> of(List<T> records, long total, long page, long pageSize) {
        return new Page<>(records, total, page, pageSize);
    }

    /**
     * 构建空分页结果
     */
    public static <T> Page<T> empty() {
        return new Page<>(Collections.emptyList(), 0, 1, 10);
    }

    /**
     * 构建空分页结果（指定页码和页大小）
     */
    public static <T> Page<T> empty(int page, int pageSize) {
        return new Page<>(Collections.emptyList(), 0, page, pageSize);
    }

    /**
     * 转换分页数据类型
     *
     * @param mapper 转换函数
     * @return 转换后的分页结果
     */
    public <R> Page<R> map(Function<T, R> mapper) {
        List<R> mappedRecords = this.records.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return new Page<>(mappedRecords, this.total, this.page, this.pageSize);
    }

    /**
     * 判断是否有数据
     */
    public boolean hasRecords() {
        return records != null && !records.isEmpty();
    }

    /**
     * 判断是否有下一页
     */
    public boolean hasNext() {
        return page < pages;
    }

    /**
     * 判断是否有上一页
     */
    public boolean hasPrevious() {
        return page > 1;
    }

    /**
     * 获取偏移量（用于 SQL LIMIT offset）
     */
    public long getOffset() {
        return (page - 1) * pageSize;
    }

}