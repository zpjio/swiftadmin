package cn.swiftadmin.system.api.dto;

import cn.swiftadmin.common.core.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限包查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionPackageQueryDTO extends PageQuery {

    /** 名称 */
    private String name;

    /** 状态 */
    private Integer status;

}