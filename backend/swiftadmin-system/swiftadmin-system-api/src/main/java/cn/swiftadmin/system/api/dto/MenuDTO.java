package cn.swiftadmin.system.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜单创建/更新 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class MenuDTO {

    private Long id;
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过 50 个字符")
    private String name;

    @Size(max = 255, message = "路由路径长度不能超过 255 个字符")
    private String path;

    @Size(max = 255, message = "组件路径长度不能超过 255 个字符")
    private String component;

    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    @Size(max = 50, message = "图标长度不能超过 50 个字符")
    private String icon;

    private Integer sortOrder;

    @Size(max = 100, message = "权限标识长度不能超过 100 个字符")
    private String permission;

    private Integer status;

}