package cn.swiftadmin.system.api.dto;

import cn.swiftadmin.common.core.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeQueryDTO extends PageQuery {

    /** 字典名称 */
    private String dictName;

    /** 字典类型 */
    private String dictType;

    /** 状态 */
    private Integer status;

}