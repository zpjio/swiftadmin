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

}