package cn.swiftadmin.system.api.dto;

import cn.swiftadmin.common.core.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigQueryDTO extends PageQuery {

    /** 配置键（模糊查询） */
    private String configKey;

    /** 配置名称（模糊查询） */
    private String configName;

    /** 配置类型 */
    private String configType;

}