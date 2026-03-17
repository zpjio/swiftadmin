package cn.swiftadmin.system.api.service;

import cn.swiftadmin.system.api.dto.UserDTO;
import cn.swiftadmin.system.api.dto.UserQueryDTO;
import cn.swiftadmin.system.api.entity.SysUser;
import cn.swiftadmin.system.api.vo.UserVO;
import com.mybatisflex.core.paginate.Page;

import java.util.List;

/**
 * 用户管理服务接口
 * <p>管理后台用户 CRUD 功能，包含数据权限校验和超级管理员保护</p>
 * <p>底层操作委托给 {@link UserService}，本接口专注于管理后台业务编排</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface UserManageService {

    /**
     * 分页查询用户
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<UserVO> page(UserQueryDTO query);

    /**
     * 获取用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    UserVO getDetail(Long id);

    /**
     * 创建用户
     *
     * @param dto 用户信息
     * @return 用户 ID
     */
    Long create(UserDTO dto);

    /**
     * 更新用户
     *
     * @param dto 用户信息
     */
    void update(UserDTO dto);

    /**
     * 删除用户
     *
     * @param ids 用户 ID 列表
     */
    void delete(List<Long> ids);

    /**
     * 更新用户状态（管理后台入口，包含超级管理员保护）
     *
     * @param userId 用户 ID
     * @param status 状态
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 重置密码（管理后台入口）
     *
     * @param userId 用户 ID
     * @param password 新密码
     */
    void resetPassword(Long userId, String password);

    /**
     * 分配角色
     *
     * @param userId 用户 ID
     * @param roleIds 角色 ID 列表
     */
    void assignRoles(Long userId, List<Long> roleIds);

}