package cn.swiftadmin.system.api.vo;

import cn.swiftadmin.common.core.enums.CommonStatus;
import lombok.Data;

/**
 * 字典类型 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class DictTypeVO {

    /** 字典类型 ID */
    private Long id;

    /** 字典名称 */
    private String dictName;

    /** 字典类型 */
    private String dictType;

    /** 状态：1=正常，0=禁用 */
    private CommonStatus status;

    /** 备注 */
    private String remark;

}