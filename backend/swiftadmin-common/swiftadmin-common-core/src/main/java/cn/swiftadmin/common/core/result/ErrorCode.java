package cn.swiftadmin.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 * <p>错误码规范：0=成功，0001xx=公共错误，010xxx=用户错误，0004xx=资源错误，0005xx=系统错误</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    /** 成功 */
    SUCCESS(0, "操作成功"),

    // ==================== 公共错误 (1xx) ====================

    /** 公共错误：参数校验失败 */
    PARAM_INVALID(100, "参数校验失败"),

    /** 公共错误：缺少必要参数 */
    PARAM_MISSING(101, "缺少必要参数"),

    // ==================== 用户错误 (1xxx) ====================

    /** 用户错误：用户不存在 */
    USER_NOT_FOUND(1001, "用户不存在"),

    /** 用户错误：用户名或密码错误 */
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),

    /** 用户错误：用户已被禁用 */
    USER_DISABLED(1003, "用户已被禁用"),

    /** 用户错误：密码强度不足 */
    PASSWORD_WEAK(1004, "密码强度不足"),

    // ==================== 验证码错误 (11xx) ====================

    /** 验证码错误：验证码错误或已过期 */
    CAPTCHA_ERROR(1101, "验证码错误或已过期"),

    /** 验证码错误：需要图形验证码 */
    CAPTCHA_REQUIRED(1102, "请先完成图形验证码验证"),

    // ==================== Token 错误 (12xx) ====================

    /** Token 错误：Token 无效或已过期 */
    TOKEN_INVALID(1201, "Token 无效或已过期"),

    /** Token 错误：Token 可能被盗用 */
    TOKEN_STOLEN(1202, "Token 可能被盗用，已失效，请重新登录"),

    /** Token 错误：刷新过于频繁 */
    TOKEN_REFRESH_TOO_FREQUENT(1203, "Token 刷新过于频繁，请稍后再试"),

    /** Token 错误：刷新次数超限 */
    TOKEN_REFRESH_LIMIT_EXCEEDED(1204, "Token 刷新次数已达上限，请明天再试"),

    // ==================== 限流错误 (13xx) ====================

    /** 限流错误：发送过于频繁 */
    RATE_LIMIT_SEND_TOO_FREQUENT(1301, "发送过于频繁，请稍后再试"),

    /** 限流错误：次数已达上限 */
    RATE_LIMIT_EXCEEDED(1302, "操作次数已达上限，请稍后再试"),

    // ==================== 登录错误 (14xx) ====================

    /** 登录错误：不支持的登录方式 */
    LOGIN_TYPE_NOT_SUPPORTED(1401, "不支持的登录方式"),

    // ==================== 短信错误 (15xx) ====================

    /** 短信错误：验证码错误或已过期 */
    INVALID_SMS_CODE(1501, "验证码错误或已过期"),

    /** 用户错误：未注册 */
    USER_NOT_REGISTERED(1502, "手机号未注册，请先注册账号"),

    // ==================== 社交登录错误 (16xx) ====================

    /** 社交登录错误：授权码无效 */
    SOCIAL_AUTH_CODE_INVALID(1601, "授权码无效或已过期"),

    /** 社交登录错误：State 参数无效 */
    SOCIAL_STATE_INVALID(1602, "State 参数无效或已过期"),

    /** 社交登录错误：平台返回错误 */
    SOCIAL_PLATFORM_ERROR(1603, "第三方平台服务异常"),

    /** 社交登录错误：账号未绑定 */
    SOCIAL_ACCOUNT_NOT_BOUND(1604, "该社交账号未绑定系统账号"),

    /** 社交登录错误：绑定已存在 */
    SOCIAL_ACCOUNT_ALREADY_BOUND(1605, "已绑定其他系统账号"),

    /** 社交登录错误：不支持的平台 */
    SOCIAL_PLATFORM_NOT_SUPPORTED(1606, "不支持的社交平台"),

    // ==================== 账户错误 (17xx) ====================

    /** 账户错误：账户已锁定 */
    ACCOUNT_LOCKED(1701, "账户已锁定，请稍后再试"),

    /** 账户错误：密码已过期 */
    PASSWORD_EXPIRED(1702, "密码已过期，请修改密码"),

    /** 账户错误：密码即将过期 */
    PASSWORD_EXPIRING_SOON(1703, "密码即将过期，请及时修改"),

    // ==================== 安全错误 (18xx) ====================

    /** 安全错误：IP 受限 */
    IP_RESTRICTED(1801, "IP 地址受限，请稍后再试"),

    /** 安全错误：设备指纹不匹配 */
    DEVICE_FINGERPRINT_MISMATCH(1802, "设备环境异常，请重新登录"),

    /** 安全错误：异地登录 */
    ABNORMAL_LOCATION_LOGIN(1803, "检测到异地登录，请确认是否为本人操作"),

    // ==================== 合规错误 (19xx) ====================

    /** 合规错误：未签署用户协议 */
    AGREEMENT_NOT_SIGNED(1901, "请先阅读并同意用户协议"),

    /** 合规错误：未同意隐私政策 */
    PRIVACY_POLICY_NOT_AGREED(1902, "请先阅读并同意隐私政策"),

    // ==================== 系统错误 (5xx) ====================

    /** 系统错误：系统异常 */
    SYSTEM_ERROR(500, "系统异常，请稍后重试"),

    /** 资源错误：资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    // ==================== 文件错误 (41x) ====================

    /** 文件错误：文件上传失败 */
    FILE_UPLOAD_FAILED(410, "文件上传失败"),

    /** 文件错误：文件不存在 */
    FILE_NOT_FOUND(411, "文件不存在"),

    /** 文件错误：文件大小超出限制 */
    FILE_SIZE_EXCEEDED(412, "文件大小超出限制"),

    /** 文件错误：不支持的文件类型 */
    FILE_TYPE_NOT_SUPPORTED(413, "不支持的文件类型"),

    /** 文件错误：文件删除失败 */
    FILE_DELETE_FAILED(414, "文件删除失败");

    /** 错误码 */
    private final int code;

    /** 错误消息 */
    private final String message;

}
