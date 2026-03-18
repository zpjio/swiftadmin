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
 * 系统配置实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_config")
public class SysConfig {

    /** 主键 ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 配置键 */
    @Column
    private String configKey;

    /** 配置值 */
    @Column
    private String configValue;

    /** 配置名称 */
    @Column
    private String configName;

    /** 配置类型 */
    @Column
    private String configType;

    /** 备注 */
    @Column
    private String remark;

    /** 创建时间 */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

}