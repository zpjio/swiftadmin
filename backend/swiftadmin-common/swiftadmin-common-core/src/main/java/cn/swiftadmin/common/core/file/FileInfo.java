package cn.swiftadmin.common.core.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件信息类
 * <p>用于封装文件上传后的返回信息</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    /**
     * 文件 ID（通常是文件名或数据库 ID）
     */
    private String fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件原始名称
     */
    private String originalFileName;

    /**
     * 文件 URL（访问地址）
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 文件 MIME 类型
     */
    private String contentType;

    /**
     * 存储类型（local/oss）
     */
    private String storageType;

    /**
     * 文件路径（OSS 中的 key 或本地路径）
     */
    private String path;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * 上传时间戳
     */
    private Long uploadTime;
}
