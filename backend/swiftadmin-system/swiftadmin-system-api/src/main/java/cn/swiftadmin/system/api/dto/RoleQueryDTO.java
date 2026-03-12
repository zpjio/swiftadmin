package cn.swiftadmin.system.api.dto;

import cn.swiftadmin.common.core.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQueryDTO extends PageQuery {

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 状态：1=正常，0=禁用 */
    private Integer status;

}