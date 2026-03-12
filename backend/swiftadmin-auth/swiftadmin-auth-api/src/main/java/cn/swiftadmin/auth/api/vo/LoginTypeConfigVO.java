package cn.swiftadmin.auth.api.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录方式配置 VO
 * <p>用于向前端返回系统支持的登录方式</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
public class LoginTypeConfigVO {

    private String code;
    private String name;
    private Boolean enabled;

}