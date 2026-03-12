package cn.swiftadmin.system.api.service;

import cn.swiftadmin.system.api.dto.RoleDTO;
import cn.swiftadmin.system.api.dto.RoleQueryDTO;
import cn.swiftadmin.system.api.entity.SysRole;
import cn.swiftadmin.system.api.vo.RoleVO;
import cn.swiftadmin.common.core.result.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface RoleService extends IService<SysRole> {

    /**
     * 分页查询角色
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<RoleVO> page(RoleQueryDTO query);

    /**
     * 获取角色详情
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    RoleVO getDetail(Long id);

    /**
     * 获取角色列表（下拉选择）
     *
     * @return 角色列表
     */
    List<RoleVO> listAll();

    /**
     * 创建角色
     *
     * @param dto 角色信息
     * @return 角色 ID
     */
    Long create(RoleDTO dto);

    /**
     * 更新角色
     *
     * @param dto 角色信息
     */
    void update(RoleDTO dto);

    /**
     * 删除角色
     *
     * @param ids 角色 ID 列表
     */
    void delete(List<Long> ids);

    /**
     * 分配菜单权限
     *
     * @param roleId 角色 ID
     * @param menuIds 菜单 ID 列表
     */
    void assignMenus(Long roleId, List<Long> menuIds);

    /**
     * 根据用户 ID 获取角色列表
     *
     * @param userId 用户 ID
     * @return 角色列表
     */
    List<SysRole> getRolesByUserId(Long userId);

    /**
     * 检查角色编码是否存在
     *
     * @param code 角色编码
     * @param excludeId 排除的角色 ID
     * @return true=存在
     */
    boolean existsByCode(String code, Long excludeId);

    /**
     * 根据角色 ID 获取菜单 ID 列表
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

}