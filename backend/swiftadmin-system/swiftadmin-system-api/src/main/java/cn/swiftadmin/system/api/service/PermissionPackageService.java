package cn.swiftadmin.system.api.service;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.system.api.dto.PermissionPackageDTO;
import cn.swiftadmin.system.api.dto.PermissionPackageQueryDTO;
import cn.swiftadmin.system.api.vo.PermissionPackageVO;

import java.util.List;

/**
 * 权限包服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface PermissionPackageService {

    /**
     * 分页查询权限包
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PermissionPackageVO> page(PermissionPackageQueryDTO query);

    /**
     * 获取权限包详情
     *
     * @param id 权限包ID
     * @return 权限包详情
     */
    PermissionPackageVO getDetail(Long id);

    /**
     * 获取所有启用的权限包列表
     *
     * @return 权限包列表
     */
    List<PermissionPackageVO> listAll();

    /**
     * 创建权限包
     *
     * @param dto 权限包DTO
     * @return 权限包ID
     */
    Long create(PermissionPackageDTO dto);

    /**
     * 更新权限包
     *
     * @param dto 权限包DTO
     */
    void update(PermissionPackageDTO dto);

    /**
     * 删除权限包
     *
     * @param ids 权限包ID列表
     */
    void delete(List<Long> ids);

    /**
     * 根据权限包ID获取菜单ID列表
     *
     * @param packageId 权限包ID
     * @return 菜单ID列表
     */
    List<Long> getMenuIdsByPackageId(Long packageId);

    /**
     * 应用权限包到角色
     * <p>将权限包中的菜单权限分配给指定角色</p>
     *
     * @param roleId 角色ID
     * @param packageId 权限包ID
     */
    void applyToRole(Long roleId, Long packageId);

    /**
     * 检查编码是否存在
     *
     * @param code 编码
     * @param excludeId 排除的ID
     * @return true=存在
     */
    boolean existsByCode(String code, Long excludeId);

}