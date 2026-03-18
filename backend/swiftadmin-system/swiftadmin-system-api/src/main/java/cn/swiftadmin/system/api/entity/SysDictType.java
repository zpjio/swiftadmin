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
 * 字典类型实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_dict_type")
public class SysDictType {

    /** 主键 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 字典名称 */
    @Column
    private String dictName;

    /** 字典类型 */
    @Column
    private String dictType;

    /** 状态 */
    @Column
    private CommonStatus status;

    /** 备注 */
    @Column
    private String remark;

    /** 创建时间 */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    /** 是否删除：0=未删除，1=已删除 */
    @Column(isLogicDelete = true)
    private Integer deleted;

}