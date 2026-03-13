package cn.swiftadmin.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证安全配置属性
 * <p>统一管理认证相关的安全配置参数，支持通过配置文件动态调整</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "auth.security")
public class AuthSecurityProperties {

    // ========== 账号密码登录配置 ==========

    /**
     * 最大登录失败次数
     * <p>连续登录失败达到此次数后锁定账户</p>
     */
    private int maxLoginFailCount = 5;

    /**
     * 账户锁定时间（分钟）
     * <p>登录失败次数超限后的锁定时长</p>
     */
    private int lockMinutes = 15;

    // ========== Token 配置 ==========

    /**
     * Token 刷新最小间隔（秒）
     * <p>同一用户两次 Token 刷新之间的最小时间间隔</p>
     */
    private int refreshIntervalSeconds = 5;

    /**
     * 单个用户每日最大 Token 刷新次数
     * <p>防止 Token 无限续期</p>
     */
    private int maxRefreshCountPerDay = 50;

    /**
     * OAuth State 有效期（分钟）
     */
    private int stateExpireMinutes = 10;

    /**
     * 临时 Token 有效期（分钟）
     * <p>用于强制修改密码等场景</p>
     */
    private int tempTokenExpireMinutes = 10;

    // ========== 短信验证码配置 ==========

    /**
     * 验证码有效期（分钟）
     */
    private int codeExpireMinutes = 5;

    /**
     * 单手机号发送间隔（秒）
     */
    private int sendIntervalSeconds = 60;

    /**
     * 单手机号 1 小时最大发送次数
     */
    private int maxSendPerHour = 5;

    /**
     * 同一 IP 1 小时最大发送次数
     */
    private int maxIpSendPerHour = 10;

    /**
     * 触发图形验证码的 IP 发送阈值
     */
    private int captchaTriggerThreshold = 3;

    /**
     * 验证码错误次数上限
     */
    private int maxVerifyFailCount = 3;

    /**
     * 验证码错误锁定时间（分钟）
     */
    private int verifyLockMinutes = 15;

    // ========== 缓存配置 ==========

    /**
     * 用户信息缓存有效期（分钟）
     */
    private int userCacheExpireMinutes = 30;

    // ========== 登录安全配置 ==========

    /**
     * 登录失败触发图形验证码的阈值
     * <p>连续登录失败达到此次数后，下次登录需要图形验证码</p>
     */
    private int captchaTriggerFailCount = 3;

    // ========== 社交 Token 配置 ==========

    /**
     * 社交 Token 刷新阈值（小时）
     * <p>即将在 N 小时内过期的 Token 将被刷新</p>
     */
    private int refreshThresholdHours = 24;

    /**
     * 社交账号绑定锁超时时间（秒）
     */
    private int bindLockTimeoutSeconds = 30;

    // ========== Token 上下文验证配置 ==========

    /**
     * Token 上下文验证模式
     * <p>STRICT: 严格模式，IP 变化直接拒绝</p>
     * <p>MODERATE: 中等模式，IP 变化记录警告，短时间内多次变化才拒绝</p>
     * <p>LOOSE: 宽松模式，IP 变化仅记录日志，不阻止</p>
     */
    private TokenContextValidationMode tokenContextValidationMode = TokenContextValidationMode.MODERATE;

    /**
     * IP 变化次数阈值（仅在 MODERATE 模式下生效）
     * <p>短时间内 IP 变化次数超过此值，视为异常</p>
     */
    private int ipChangeThreshold = 3;

    /**
     * IP 变化统计时间窗口（分钟）
     * <p>在此时间窗口内统计 IP 变化次数</p>
     */
    private int ipChangeWindowMinutes = 30;

    /**
     * Token 上下文验证模式枚举
     */
    public enum TokenContextValidationMode {
        /** 严格模式：IP 变化直接拒绝 */
        STRICT,
        /** 中等模式：短时间多次 IP 变化才拒绝 */
        MODERATE,
        /** 宽松模式：仅记录日志，不阻止 */
        LOOSE
    }

    /**
     * 配置参数校验
     * <p>在 Bean 初始化后自动执行，确保配置参数合法</p>
     */
    @PostConstruct
    public void validate() {
        log.info("开始验证认证安全配置参数...");
        
        validateRange("maxLoginFailCount", maxLoginFailCount, 1, 20, "最大登录失败次数");
        validateRange("lockMinutes", lockMinutes, 1, 1440, "账户锁定时间");
        validateRange("refreshIntervalSeconds", refreshIntervalSeconds, 1, 300, "Token 刷新间隔");
        validateRange("maxRefreshCountPerDay", maxRefreshCountPerDay, 1, 200, "每日最大刷新次数");
        validateRange("stateExpireMinutes", stateExpireMinutes, 1, 60, "OAuth State 有效期");
        validateRange("tempTokenExpireMinutes", tempTokenExpireMinutes, 1, 60, "临时 Token 有效期");
        validateRange("codeExpireMinutes", codeExpireMinutes, 1, 60, "验证码有效期");
        validateRange("sendIntervalSeconds", sendIntervalSeconds, 10, 600, "短信发送间隔");
        validateRange("maxSendPerHour", maxSendPerHour, 1, 20, "单手机号每小时发送次数");
        validateRange("maxIpSendPerHour", maxIpSendPerHour, 1, 50, "IP 每小时发送次数");
        validateRange("captchaTriggerThreshold", captchaTriggerThreshold, 1, 10, "图形验证码触发阈值");
        validateRange("maxVerifyFailCount", maxVerifyFailCount, 1, 10, "验证码错误次数上限");
        validateRange("verifyLockMinutes", verifyLockMinutes, 1, 120, "验证码错误锁定时间");
        validateRange("userCacheExpireMinutes", userCacheExpireMinutes, 1, 1440, "用户缓存有效期");
        validateRange("captchaTriggerFailCount", captchaTriggerFailCount, 1, 10, "登录失败触发图形验证码阈值");
        validateRange("refreshThresholdHours", refreshThresholdHours, 1, 72, "社交 Token 刷新阈值");
        validateRange("bindLockTimeoutSeconds", bindLockTimeoutSeconds, 1, 300, "绑定锁超时时间");
        validateRange("ipChangeThreshold", ipChangeThreshold, 1, 10, "IP 变化次数阈值");
        validateRange("ipChangeWindowMinutes", ipChangeWindowMinutes, 1, 1440, "IP 变化统计时间窗口");
        
        log.info("认证安全配置参数验证通过");
    }

    /**
     * 验证参数范围
     *
     * @param paramName 参数名
     * @param value 参数值
     * @param min 最小值
     * @param max 最大值
     * @param description 参数描述
     * @throws IllegalArgumentException 参数超出范围时抛出
     */
    private void validateRange(String paramName, int value, int min, int max, String description) {
        if (value < min || value > max) {
            String message = String.format(
                    "配置参数错误：%s (%s) 的值为 %d，有效范围为 [%d, %d]",
                    paramName, description, value, min, max);
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        log.debug("配置参数验证通过：{} = {}", paramName, value);
    }
}
