package cn.swiftadmin.system.api.vo;

import cn.swiftadmin.common.core.enums.CommonStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门详情 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class DeptVO {

    /** 部门 ID */
    private Long id;

    /** 父部门 ID */
    private Long parentId;

    /** 部门名称 */
    private String name;

    /** 排序 */
    private Integer sortOrder;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态：1=正常，0=禁用 */
    private CommonStatus status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 子部门 */
    private List<DeptVO> children;

}