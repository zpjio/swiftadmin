package cn.swiftadmin.common.mybatis.util;

import com.mybatisflex.core.paginate.Page;
import org.springframework.util.StringUtils;

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

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 最大每页大小（防止恶意大分页请求） */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 创建分页请求（带参数校验）
     * <p>自动处理空值、负值，限制最大 pageSize</p>
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return MyBatis-Flex 分页对象
     */
    public static <T> Page<T> of(Integer page, Integer pageSize) {
        int p = page != null && page > 0 ? page : DEFAULT_PAGE;
        int ps = pageSize != null && pageSize > 0 ? Math.min(pageSize, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        return new Page<>(p, ps);
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

}