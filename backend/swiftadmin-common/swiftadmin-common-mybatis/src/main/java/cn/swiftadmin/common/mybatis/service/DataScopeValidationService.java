package cn.swiftadmin.common.mybatis.service;

/**
 * 数据权限校验服务接口
 * <p>提供数据权限校验能力，用于 UPDATE/DELETE 操作前的权限检查</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface DataScopeValidationService {

    /**
     * 校验用户是否有权限操作指定用户的数据
     *
     * @param targetUserId 目标用户ID
     * @return true=有权限
     */
    boolean canAccessUserData(Long targetUserId);

    /**
     * 校验用户是否有权限操作指定部门的数据
     *
     * @param targetDeptId 目标部门ID
     * @return true=有权限
     */
    boolean canAccessDeptData(Long targetDeptId);

    /**
     * 校验用户是否有权限操作指定部门及其子部门的数据
     *
     * @param targetDeptId 目标部门ID
     * @return true=有权限
     */
    boolean canAccessDeptDataWithChildren(Long targetDeptId);

    /**
     * 校验当前用户是否为数据的所有者
     *
     * @param targetUserId 目标用户ID
     * @return true=是所有者
     */
    boolean isDataOwner(Long targetUserId);

    /**
     * 校验数据权限（根据当前用户的数据权限范围自动判断）
     *
     * @param targetUserId 目标用户ID
     * @param targetDeptId 目标部门ID（可为null）
     * @return true=有权限
     */
    boolean validateDataScope(Long targetUserId, Long targetDeptId);
}