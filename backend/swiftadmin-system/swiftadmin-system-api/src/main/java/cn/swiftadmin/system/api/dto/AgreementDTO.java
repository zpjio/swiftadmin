package cn.swiftadmin.system.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户协议 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class AgreementDTO {

    /** 协议 ID（更新时必填） */
    private Long id;

    /** 协议类型：agreement=用户协议，privacy=隐私政策 */
    @NotBlank(message = "协议类型不能为空")
    private String type;

    /** 版本号 */
    @NotBlank(message = "版本号不能为空")
    @Size(max = 50, message = "版本号长度不能超过50")
    private String version;

    /** 协议标题 */
    @NotBlank(message = "协议标题不能为空")
    @Size(max = 200, message = "协议标题长度不能超过200")
    private String title;

    /** 协议内容 */
    @NotBlank(message = "协议内容不能为空")
    private String content;

    /** 状态：0=停用，1=启用 */
    private Integer status;

    /** 生效时间 */
    private LocalDateTime effectiveDate;

}