package cn.swiftadmin.common.core.service;

/**
 * 当前用户上下文服务接口
 * <p>提供获取当前登录用户信息的能力</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface CurrentUserService {

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID，未登录返回null
     */
    Long getCurrentUserId();

    /**
     * 获取当前登录用户名
     *
     * @return 用户名，未登录返回null
     */
    String getCurrentUsername();
}