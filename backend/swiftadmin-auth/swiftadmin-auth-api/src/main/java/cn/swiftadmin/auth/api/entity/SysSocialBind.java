package cn.swiftadmin.auth.api.entity;

import cn.swiftadmin.common.mybatis.encrypt.AesGcmTypeHandler;
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
 * 用户社交账号绑定实体
 * <p>存储系统与第三方社交账号的绑定关系，支持企业微信、钉钉、飞书等</p>
 * <p>Token 字段加密方案：</p>
 * <ul>
 *   <li>SetListener：在设置字段值时自动加密（包括 UpdateChain）</li>
 *   <li>TypeHandler：在从数据库读取时自动解密</li>
 * </ul>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "sys_social_bind", onSet = {SocialBindEncryptSetListener.class})
public class SysSocialBind {

    /** 主键 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 系统用户 ID */
    @Column
    private Long userId;

    /** 来源：wecom|dingtalk|feishu */
    @Column
    private String source;

    /** 第三方用户 OpenID */
    @Column
    private String openId;

    /** 第三方统一 UnionID（可选） */
    @Column
    private String unionId;

    /** 访问 Token（加密存储） */
    @Column(typeHandler = AesGcmTypeHandler.class)
    private String accessToken;

    /** 刷新 Token（加密存储） */
    @Column(typeHandler = AesGcmTypeHandler.class)
    private String refreshToken;

    /** Token 过期时间 */
    @Column
    private LocalDateTime tokenExpiresAt;

    /** 额外信息（昵称、头像等 JSON） */
    @Column
    private String extraInfo;

    /** 绑定时间 */
    @Column
    private LocalDateTime bindTime;

    /** 更新时间 */
    @Column
    private LocalDateTime updateTime;

}