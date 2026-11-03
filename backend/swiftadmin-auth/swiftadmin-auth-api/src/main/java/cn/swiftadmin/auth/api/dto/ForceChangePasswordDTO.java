package cn.swiftadmin.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 使用临时令牌强制修改密码。
 * <p>临时令牌和新密码都放在 JSON 请求体中</p>
 */
@Data
public class ForceChangePasswordDTO {

    @NotBlank(message = "临时令牌不能为空")
    private String tempToken;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
