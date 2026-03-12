package cn.swiftadmin.system.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门创建/更新 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class DeptDTO {

    private Long id;
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称长度不能超过 50 个字符")
    private String name;

    private Integer sortOrder;

    @Size(max = 50, message = "负责人长度不能超过 50 个字符")
    private String leader;

    @Size(max = 20, message = "联系电话长度不能超过 20 个字符")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
    private String email;

    private Integer status;

}