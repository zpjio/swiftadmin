package cn.swiftadmin.system.api.entity;

import cn.swiftadmin.common.core.enums.CommonStatus;
import cn.swiftadmin.common.core.enums.YesNo;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.ColumnMask;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 * <p>存储用户基本信息，支持软删除</p>
 * <p>使用 MyBatis-Flex 注解实现逻辑删除自动处理</p>
 * <p>使用 @ColumnMask 注解实现敏感数据脱敏</p>
 * <p>使用枚举类型增强类型安全</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_user")
public class SysUser {

    /** 用户 ID */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 用户名（登录账号） */
    @Column
    private String username;

    /** 密码（BCrypt 加密） */
    @Column
    private String password;

    /** 昵称 */
    @Column
    private String nickname;

    /** 邮箱 */
    @ColumnMask("email")
    @Column
    private String email;

    /** 手机号 */
    @ColumnMask("mobile")
    @Column
    private String phone;

    /** 头像 URL */
    @Column
    private String avatar;

    /** 状态 */
    @Column
    private CommonStatus status;

    /** 部门 ID */
    @Column
    private Long deptId;

    /** 是否超级管理员 */
    @Column
    private YesNo isSuper;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column
    private LocalDateTime updateTime;

    /** 是否删除：0=未删除，1=已删除 */
    @Column(isLogicDelete = true)
    private Integer deleted;

    /** 登录失败次数 */
    @Column
    private Integer loginFailCount;

    /** 账户锁定时间（锁定到期时间） */
    @Column
    private LocalDateTime lockTime;

    /** 密码最后更新时间 */
    @Column
    private LocalDateTime passwordUpdateTime;

    /** 是否强制修改密码 */
    @Column
    private YesNo forceChangePassword;

}