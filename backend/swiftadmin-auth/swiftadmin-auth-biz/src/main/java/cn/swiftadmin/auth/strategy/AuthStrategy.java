package cn.swiftadmin.auth.strategy;

import cn.swiftadmin.auth.api.dto.LoginDTO;
import cn.swiftadmin.auth.api.vo.LoginVO;
import cn.swiftadmin.auth.api.enums.LoginType;

/**
 * 认证策略接口
 * <p>定义统一的认证流程，支持多种登录方式</p>
 * <p>使用 default 方法提供扩展点，支持社交登录等特殊场景</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface AuthStrategy {

    /**
     * 支持的登录类型
     *
     * @return 登录类型枚举
     */
    LoginType getLoginType();

    /**
     * 执行认证
     *
     * @param dto 登录参数
     * @return 登录结果
     */
    LoginVO authenticate(LoginDTO dto);

    // ========== 社交登录扩展方法（default 实现） ==========

    /**
     * 获取社交登录授权 URL
     * <p>仅社交登录策略需要实现</p>
     *
     * @param state 状态码（CSRF 防护）
     * @return 授权 URL
     */
    default String getAuthUrl(String state) {
        throw new UnsupportedOperationException("当前登录方式不支持获取授权 URL");
    }

    /**
     * 绑定社交账号
     * <p>仅社交登录策略需要实现</p>
     *
     * @param userId 系统用户 ID
     * @param tempToken 临时 Token
     */
    default void bindAccount(Long userId, String tempToken) {
        throw new UnsupportedOperationException("当前登录方式不支持绑定账号");
    }

    /**
     * 是否支持账号绑定
     *
     * @return true=支持，false=不支持
     */
    default boolean supportsBind() {
        return false;
    }

    /**
     * 获取登录方式来源标识
     * <p>仅社交登录策略需要实现</p>
     *
     * @return 来源标识（如 wecom、dingtalk、feishu）
     */
    default String getSource() {
        return getLoginType().getCode();
    }

}
