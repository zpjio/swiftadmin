package cn.swiftadmin.auth.api.service;

import cn.swiftadmin.auth.api.entity.SysSocialBind;

import java.util.List;
import java.util.Map;

/**
 * 社交账号绑定服务接口
 * <p>处理用户与第三方社交账号的绑定、解绑、查询等操作</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface SocialBindService {

    /**
     * 根据来源和 OpenID 查询绑定关系
     *
     * @param source 来源（wecom|dingtalk|feishu）
     * @param openId 第三方用户 OpenID
     * @return 绑定记录，不存在返回 null
     */
    SysSocialBind getBySourceAndOpenId(String source, String openId);

    /**
     * 根据用户 ID 和来源查询绑定关系
     *
     * @param userId 系统用户 ID
     * @param source 来源（wecom|dingtalk|feishu）
     * @return 绑定记录，不存在返回 null
     */
    SysSocialBind getByUserIdAndSource(Long userId, String source);

    /**
     * 查询用户的所有绑定关系
     *
     * @param userId 系统用户 ID
     * @return 绑定记录列表
     */
    List<SysSocialBind> getByUserId(Long userId);

    /**
     * 绑定社交账号
     *
     * @param userId 系统用户 ID
     * @param source 来源（wecom|dingtalk|feishu）
     * @param openId 第三方用户 OpenID
     * @param userInfo 第三方用户信息（昵称、头像等）
     */
    void bind(Long userId, String source, String openId, Map<String, Object> userInfo);

    /**
     * 解绑社交账号
     *
     * @param userId 系统用户 ID
     * @param source 来源（wecom|dingtalk|feishu）
     */
    void unbind(Long userId, String source);

    /**
     * 更新社交账号 Token
     *
     * @param bindId 绑定记录 ID
     * @param userInfo 包含 Token 信息的用户数据
     */
    void updateToken(Long bindId, Map<String, Object> userInfo);

    /**
     * 检查来源和 OpenID 是否已被绑定
     *
     * @param source 来源
     * @param openId OpenID
     * @return true=已绑定
     */
    boolean isBound(String source, String openId);

}
