package cn.swiftadmin.system.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限包 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class PermissionPackageVO {

    /** 权限包 ID */
    private Long id;

    /** 权限包名称 */
    private String name;

    /** 权限包编码 */
    private String code;

    /** 描述 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 菜单 ID 列表 */
    private List<Long> menuIds;

    /** 菜单数量 */
    private Integer menuCount;

}