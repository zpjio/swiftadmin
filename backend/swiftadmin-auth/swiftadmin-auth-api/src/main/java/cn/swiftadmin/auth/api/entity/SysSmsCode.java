package cn.swiftadmin.auth.api.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 短信验证码实体
 * <p>存储短信验证码及其使用状态，支持登录、注册、绑定等场景</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_sms_code")
public class SysSmsCode {

    /** 主键 ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 手机号 */
    @Column
    private String phone;

    /** 验证码（6 位数字） */
    @Column
    private String code;

    /** 使用场景：login|register|bind */
    @Column
    private String scene;

    /** 状态：1=未使用，0=已使用 */
    @Column
    private Integer status;

    /** 过期时间 */
    @Column
    private LocalDateTime expireAt;

    /** 请求 IP 地址 */
    @Column
    private String ipAddress;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

}
