package cn.swiftadmin.auth.strategy.social;

import cn.swiftadmin.auth.api.dto.SocialUserInfo;

/**
 * 社交登录处理器接口
 * <p>定义社交登录平台的核心操作，支持快速扩展新的社交登录平台</p>
 * <p>实现此接口并添加 @Component 注解即可自动注册到系统中</p>
 *
 * <h3>扩展示例：</h3>
 * <pre>
 * &#64;Component
 * public class GithubAuthHandler implements SocialAuthHandler {
 *     &#64;Override
 *     public String getSource() { return "github"; }
 *
 *     &#64;Override
 *     public String getAuthUrl(String state) {
 *         return "https://github.com/login/oauth/authorize?...";
 *     }
 *
 *     &#64;Override
 *     public SocialUserInfo getUserInfo(String authCode) {
 *         // 调用 GitHub API 获取用户信息
 *     }
 * }
 * </pre>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface SocialAuthHandler {

    /**
     * 获取社交平台来源标识
     * <p>用于匹配登录请求和绑定关系</p>
     *
     * @return 来源标识（如 wecom、dingtalk、feishu、github）
     */
    String getSource();

    /**
     * 获取社交登录授权 URL
     *
     * @param state 状态码（用于 CSRF 防护，需在回调时验证）
     * @return 第三方平台授权跳转 URL
     */
    String getAuthUrl(String state);

    /**
     * 使用授权码换取用户信息
     *
     * @param authCode 第三方平台返回的授权码
     * @return 标准化的用户信息
     */
    SocialUserInfo getUserInfo(String authCode);

    /**
     * 从用户信息中提取 OpenID
     * <p>不同平台的字段名可能不同，由各 Handler 自行处理</p>
     *
     * @param userInfo 用户信息
     * @return OpenID
     */
    default String extractOpenId(SocialUserInfo userInfo) {
        String openId = userInfo.getOpenId();
        if (openId == null) {
            openId = userInfo.getUnionId();
        }
        return openId;
    }

}