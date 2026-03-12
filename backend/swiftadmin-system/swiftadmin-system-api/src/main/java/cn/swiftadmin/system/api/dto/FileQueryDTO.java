package cn.swiftadmin.system.api.dto;

import cn.swiftadmin.common.core.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件查询 DTO
 *
 * @author zhangpengju
 * @since 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileQueryDTO extends PageQuery {

    /** 文件名 */
    private String fileName;

    /** 业务类型 */
    private String bizType;

    /** 存储类型 */
    private String storageType;

    /** 状态 */
    private Integer status;

}