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
 * 部门实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_dept")
public class SysDept {

    /** 主键 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 父部门 ID */
    @Column
    private Long parentId;

    /** 祖级路径（逗号分隔） */
    @Column
    private String ancestors;

    /** 部门名称 */
    @Column
    private String name;

    /** 排序 */
    @Column
    private Integer sortOrder;

    /** 负责人 */
    @Column
    private String leader;

    /** 联系电话 */
    @Column
    private String phone;

    /** 邮箱 */
    @Column
    private String email;

    /** 状态 */
    @Column
    private CommonStatus status;

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