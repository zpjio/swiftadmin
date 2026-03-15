package cn.swiftadmin.system.api.entity;

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
 * 密码历史记录实体
 * <p>存储用户历史密码，防止重复使用</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_password_history")
public class SysPasswordHistory {

    /** 主键 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 用户 ID */
    @Column
    private Long userId;

    /** 密码哈希值（BCrypt） */
    @Column
    private String passwordHash;

    /** 创建时间 */
    @Column
    private LocalDateTime createTime;

}