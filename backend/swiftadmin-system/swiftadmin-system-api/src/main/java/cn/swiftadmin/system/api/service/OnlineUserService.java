package cn.swiftadmin.system.api.service;

import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.system.api.vo.OnlineUserVO;

import java.util.List;

/**
 * 在线用户服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface OnlineUserService {

    /**
     * 分页查询在线用户
     *
     * @param username 用户名（模糊查询）
     * @param ip 登录IP
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<OnlineUserVO> page(String username, String ip, Integer page, Integer pageSize);

    /**
     * 获取所有在线用户
     *
     * @return 在线用户列表
     */
    List<OnlineUserVO> listAll();

    /**
     * 强制用户下线
     *
     * @param sessionId 会话ID
     * @return true=成功
     */
    boolean forceLogout(String sessionId);

    /**
     * 批量强制用户下线
     *
     * @param sessionIds 会话ID列表
     * @return 成功数量
     */
    int forceLogoutBatch(List<String> sessionIds);

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    long count();

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return true=在线
     */
    boolean isOnline(Long userId);

}