package cn.swiftadmin.system.api.vo;

import cn.swiftadmin.common.core.enums.CommonStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色详情 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class RoleVO {

    /** 角色 ID */
    private Long id;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 描述 */
    private String description;

    /** 状态：1=正常，0=禁用 */
    private CommonStatus status;

    /** 数据范围：1=全部，2=自定义，3=本部门，4=本部门及以下，5=仅本人 */
    private Integer dataScope;

    /** 菜单 ID 列表 */
    private List<Long> menuIds;

    /** 部门 ID 列表 */
    private List<Long> deptIds;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}