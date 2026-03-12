package cn.swiftadmin.system.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统配置 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class ConfigDTO {

    private Long id;

    @NotBlank(message = "配置键不能为空")
    @Size(max = 100, message = "配置键长度不能超过 100")
    private String configKey;

    @Size(max = 500, message = "配置值长度不能超过 500")
    private String configValue;

    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称长度不能超过 100")
    private String configName;

    @Size(max = 50, message = "配置类型长度不能超过 50")
    private String configType;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;

}