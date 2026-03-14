package cn.swiftadmin.common.mybatis.service;

/**
 * 用户信息查询服务接口
 * <p>用于数据权限校验时查询用户部门信息</p>
 * <p>由 swiftadmin-system-biz 模块提供实现</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
public interface UserInfoQueryService {

    /**
     * 根据用户ID查询部门ID
     *
     * @param userId 用户ID
     * @return 部门ID，不存在返回 null
     */
    Long getDeptIdByUserId(Long userId);

    /**
     * 根据用户ID查询用户名
     *
     * @param userId 用户ID
     * @return 用户名，不存在返回 null
     */
    String getUsernameByUserId(Long userId);

    /**
     * 获取部门的祖级路径
     *
     * @param deptId 部门ID
     * @return 祖级路径（逗号分隔），不存在返回 null
     */
    String getDeptAncestors(Long deptId);

    /**
     * 判断目标部门是否是当前部门的子部门（或自身）
     * <p>用于"本部门及以下"数据权限校验</p>
     *
     * @param targetDeptId 目标部门ID
     * @param currentDeptId 当前部门ID
     * @return 是子部门或自身返回 true
     */
    boolean isChildDeptOrSelf(Long targetDeptId, Long currentDeptId);

}