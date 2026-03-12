package cn.swiftadmin.system.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户创建/更新 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class UserDTO {

    private Long id;

    @NotBlank(message = "用户名不能为空", groups = {Create.class})
    @Size(min = 3, max = 20, message = "用户名长度必须在 3-20 个字符之间")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "用户名必须以字母开头，只能包含字母、数字、下划线")
    private String username;

    @NotBlank(message = "密码不能为空", groups = {Create.class})
    @Size(min = 8, max = 32, message = "密码长度必须在 8-32 个字符之间")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
    private String email;

    /**
     * 手机号
     * <p>支持两种格式：正常手机号 或 脱敏格式（如 138****0000）</p>
     * <p>脱敏格式用于前端回显，后端会自动跳过脱敏值的更新</p>
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$|^\\d{3}\\*{4}\\d{4}$", message = "手机号格式不正确")
    private String phone;

    private String avatar;
    private Integer status;
    private Long deptId;
    private List<Long> roleIds;

    public interface Create {}
    public interface Update {}

}