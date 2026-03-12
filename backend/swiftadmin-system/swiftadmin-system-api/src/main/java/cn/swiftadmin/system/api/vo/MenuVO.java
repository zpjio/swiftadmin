package cn.swiftadmin.system.api.vo;

import cn.swiftadmin.common.core.enums.CommonStatus;
import cn.swiftadmin.common.core.enums.MenuType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单详情 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class MenuVO {

    /** 菜单 ID */
    private Long id;

    /** 父菜单 ID */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 类型：1=目录，2=菜单，3=按钮 */
    private MenuType type;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    /** 权限标识 */
    private String permission;

    /** 状态：1=正常，0=禁用 */
    private CommonStatus status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 子菜单 */
    private List<MenuVO> children;

}