package cn.swiftadmin.system.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息 VO
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileVO {

    /** 文件ID */
    private Long id;

    /** 文件名称 */
    private String fileName;

    /** 存储文件名 */
    private String storageName;

    /** 文件路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件大小（格式化） */
    private String fileSizeFormat;

    /** 文件类型 */
    private String fileType;

    /** 文件扩展名 */
    private String fileExtension;

    /** 存储类型 */
    private String storageType;

    /** 访问URL */
    private String accessUrl;

    /** 业务类型 */
    private String bizType;

    /** 上传用户名 */
    private String uploadUserName;

    /** 下载次数 */
    private Integer downloadCount;

    /** 状态 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}