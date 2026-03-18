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
 * 系统文件实体
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_file")
public class SysFile {

    /** 文件ID */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    private Long id;

    /** 文件名称（原始文件名） */
    @Column
    private String fileName;

    /** 存储文件名（UUID） */
    @Column
    private String storageName;

    /** 文件路径 */
    @Column
    private String filePath;

    /** 文件大小（字节） */
    @Column
    private Long fileSize;

    /** 文件类型（MIME类型） */
    @Column
    private String fileType;

    /** 文件扩展名 */
    @Column
    private String fileExtension;

    /** 存储类型（local/oss） */
    @Column
    private String storageType;

    /** 文件MD5 */
    @Column
    private String fileMd5;

    /** 访问URL */
    @Column
    private String accessUrl;

    /** 业务类型（avatar/attachment等） */
    @Column
    private String bizType;

    /** 业务ID */
    @Column
    private Long bizId;

    /** 上传用户ID */
    @Column
    private Long uploadUserId;

    /** 上传用户名 */
    @Column
    private String uploadUserName;

    /** 下载次数 */
    @Column
    private Integer downloadCount;

    /** 状态（0禁用 1正常） */
    @Column
    private Integer status;

    /** 备注 */
    @Column
    private String remark;

    /** 创建时间 */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    /** 删除标志 */
    @Column(isLogicDelete = true)
    private Integer deleted;
}