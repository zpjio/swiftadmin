package cn.swiftadmin.system.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色创建/更新 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class RoleDTO {

    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过 50 个字符")
    private String name;

    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "角色编码必须以小写字母开头，只能包含小写字母、数字、下划线")
    @Size(max = 50, message = "角色编码长度不能超过 50 个字符")
    private String code;

    @Size(max = 255, message = "描述长度不能超过 255 个字符")
    private String description;

    private Integer status;
    private Integer dataScope;
    private Long packageId;
    private List<Long> menuIds;
    private List<Long> deptIds;

}