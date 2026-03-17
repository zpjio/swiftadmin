package cn.swiftadmin.common.security.service;

import cn.swiftadmin.common.security.domain.LoginUser;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户会话服务接口
 * <p>管理用户登录会话，支持在线用户监控</p>
 *
 * @author zhangpengju
 * @since 2026-03-12
 */
public interface UserSessionService {

    /**
     * 创建用户会话
     *
     * @param sessionId 会话ID（通常是 JWT 的 jti）
     * @param loginUser 登录用户信息
     * @param ip 登录IP
     * @param userAgent User-Agent
     * @param expireSeconds 过期时间（秒）
     */
    void createSession(String sessionId, LoginUser loginUser, String ip, String userAgent, long expireSeconds);

    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 登录用户信息，不存在返回 null
     */
    LoginUser getSession(String sessionId);

    /**
     * 获取会话扩展信息
     *
     * @param sessionId 会话ID
     * @return 会话扩展信息
     */
    SessionExtra getSessionExtra(String sessionId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return true=删除成功
     */
    boolean removeSession(String sessionId);

    /**
     * 批量删除会话
     *
     * @param sessionIds 会话ID列表
     * @return 删除成功的数量
     */
    int removeSessions(List<String> sessionIds);

    /**
     * 获取用户的所有会话ID
     *
     * @param userId 用户ID
     * @return 会话ID列表
     */
    List<String> getUserSessionIds(Long userId);

    /**
     * 获取所有会话ID
     *
     * @return 所有会话ID列表
     */
    List<String> getAllSessionIds();

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    long count();

    /**
     * 刷新会话过期时间
     *
     * @param sessionId 会话ID
     * @param expireSeconds 新的过期时间（秒）
     */
    void refreshSession(String sessionId, long expireSeconds);

    /**
     * 会话扩展信息
     */
    record SessionExtra(
            String ip,
            String location,
            String browser,
            String os,
            LocalDateTime loginTime,
            LocalDateTime expireTime
    ) {}
}
