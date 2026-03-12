package cn.swiftadmin.system.api.service;

import cn.swiftadmin.system.api.dto.MenuDTO;
import cn.swiftadmin.system.api.entity.SysMenu;
import cn.swiftadmin.system.api.vo.MenuTreeVO;
import cn.swiftadmin.system.api.vo.MenuVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface MenuService extends IService<SysMenu> {

    /**
     * 获取菜单树
     *
     * @param status 状态过滤（可选）
     * @return 菜单树
     */
    List<MenuVO> getTree(Integer status);

    /**
     * 获取菜单详情
     *
     * @param id 菜单 ID
     * @return 菜单详情
     */
    MenuVO getDetail(Long id);

    /**
     * 创建菜单
     *
     * @param dto 菜单信息
     * @return 菜单 ID
     */
    Long create(MenuDTO dto);

    /**
     * 更新菜单
     *
     * @param dto 菜单信息
     */
    void update(MenuDTO dto);

    /**
     * 删除菜单
     *
     * @param id 菜单 ID
     */
    void delete(Long id);

    /**
     * 获取菜单下拉树
     *
     * @return 菜单树（用于下拉选择）
     */
    List<MenuTreeVO> getSelectTree();

    /**
     * 获取用户菜单
     *
     * @param userId 用户 ID
     * @return 菜单树
     */
    List<MenuVO> getUserMenu(Long userId);

    /**
     * 根据用户 ID 获取菜单列表
     *
     * @param userId 用户 ID
     * @return 菜单列表
     */
    List<SysMenu> getMenusByUserId(Long userId);

    /**
     * 根据角色 ID 获取菜单 ID 列表
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

}