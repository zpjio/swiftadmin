package cn.swiftadmin.system.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线用户 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
public class OnlineUserVO {

    /** 会话ID */
    private String sessionId;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 部门名称 */
    private String deptName;

    /** 登录IP */
    private String ip;

    /** 登录地点 */
    private String location;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** Token过期时间 */
    private LocalDateTime expireTime;

    /** 剩余有效期（秒） */
    private Long ttl;

}