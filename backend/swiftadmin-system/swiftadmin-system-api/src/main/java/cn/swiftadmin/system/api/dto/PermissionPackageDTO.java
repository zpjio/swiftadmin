package cn.swiftadmin.system.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 权限包 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class PermissionPackageDTO {

    /** 权限包 ID */
    private Long id;

    /** 权限包名称 */
    @NotBlank(message = "权限包名称不能为空")
    @Size(max = 100, message = "权限包名称长度不能超过100")
    private String name;

    /** 权限包编码（可选，不填则自动生成） */
    @Size(max = 100, message = "权限包编码长度不能超过100")
    private String code;

    /** 描述 */
    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    /** 菜单 ID 列表 */
    private List<Long> menuIds;

}