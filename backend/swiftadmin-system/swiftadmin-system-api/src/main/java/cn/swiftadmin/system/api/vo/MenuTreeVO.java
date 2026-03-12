package cn.swiftadmin.system.api.vo;

import lombok.Data;

/**
 * 菜单树 VO（用于下拉选择）
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class MenuTreeVO {

    /** 菜单 ID */
    private Long id;

    /** 父菜单 ID */
    private Long parentId;

    /** 菜单名称 */
    private String label;

    /** 类型：1=目录，2=菜单，3=按钮 */
    private Integer type;

    /** 子菜单 */
    private java.util.List<MenuTreeVO> children;

    /**
     * 静态工厂方法
     */
    public static MenuTreeVO of(Long id, Long parentId, String label, Integer type) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(id);
        vo.setParentId(parentId);
        vo.setLabel(label);
        vo.setType(type);
        return vo;
    }

}