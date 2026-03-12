package cn.swiftadmin.auth.api.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 登录方式枚举
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Getter
@AllArgsConstructor
public enum LoginType {

    PASSWORD("password", "账号密码登录"),
    SMS("sms", "手机验证码登录"),
    WECOM("wecom", "企业微信登录"),
    DINGTALK("dingtalk", "钉钉登录"),
    FEISHU("feishu", "飞书登录");

    @EnumValue
    private final String code;
    private final String description;

    public static LoginType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

}