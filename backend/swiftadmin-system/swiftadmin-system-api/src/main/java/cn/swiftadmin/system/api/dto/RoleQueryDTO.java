package cn.swiftadmin.system.api.dto;

import lombok.Data;

/**
 * 角色查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class RoleQueryDTO {

    /** 当前页码 */
    private Integer page = 1;

    /** 每页大小 */
    private Integer pageSize = 10;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 状态：1=正常，0=禁用 */
    private Integer status;

}