package cn.swiftadmin.system.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class ConfigVO {

    /** 配置 ID */
    private Long id;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置名称 */
    private String configName;

    /** 配置类型 */
    private String configType;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}