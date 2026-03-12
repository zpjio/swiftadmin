package cn.swiftadmin.system.api.dto;

import lombok.Data;

/**
 * 系统配置查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class ConfigQueryDTO {

    /** 配置键（模糊查询） */
    private String configKey;

    /** 配置名称（模糊查询） */
    private String configName;

    /** 配置类型 */
    private String configType;

    /** 页码 */
    private Integer page = 1;

    /** 每页大小 */
    private Integer pageSize = 10;

}