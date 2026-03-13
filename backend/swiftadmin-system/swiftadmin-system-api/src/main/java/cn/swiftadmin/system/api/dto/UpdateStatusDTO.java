package cn.swiftadmin.system.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新状态 DTO
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Data
public class UpdateStatusDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值无效，有效值为 0(禁用) 或 1(启用)")
    @Max(value = 1, message = "状态值无效，有效值为 0(禁用) 或 1(启用)")
    private Integer status;

}