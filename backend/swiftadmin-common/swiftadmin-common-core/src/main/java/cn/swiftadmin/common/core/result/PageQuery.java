package cn.swiftadmin.common.core.result;

import lombok.Data;

/**
 * 分页查询基类
 * <p>所有分页查询 DTO 继承此类，统一分页参数处理</p>
 *
 * @author zhangpengju
 * @since 2026-03-12
 */
@Data
public class PageQuery {

    /** 当前页码 */
    private Integer page = 1;

    /** 每页大小 */
    private Integer pageSize = 10;

}