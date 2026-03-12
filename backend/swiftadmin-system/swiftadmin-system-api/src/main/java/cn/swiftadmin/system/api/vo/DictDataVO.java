package cn.swiftadmin.system.api.vo;

import cn.swiftadmin.common.core.enums.CommonStatus;
import lombok.Data;

/**
 * 字典数据 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class DictDataVO {

    /** 字典数据 ID */
    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 字典标签 */
    private String label;

    /** 字典值 */
    private String value;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1=正常，0=禁用 */
    private CommonStatus status;

    /** 备注 */
    private String remark;

}