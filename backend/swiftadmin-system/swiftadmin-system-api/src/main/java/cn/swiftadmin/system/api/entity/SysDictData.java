package cn.swiftadmin.system.api.entity;

import cn.swiftadmin.common.core.enums.CommonStatus;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 字典数据实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_dict_data")
public class SysDictData {

    /** 字典数据 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 字典类型 */
    @Column
    private String dictType;

    /** 字典标签 */
    @Column("dict_label")
    private String label;

    /** 字典值 */
    @Column("dict_value")
    private String value;

    /** 排序 */
    @Column
    private Integer sortOrder;

    /** 状态 */
    @Column
    private CommonStatus status;

    /** 备注 */
    @Column
    private String remark;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column
    private LocalDateTime updateTime;

    /** 是否删除：0=未删除，1=已删除 */
    @Column(isLogicDelete = true)
    private Integer deleted;

}