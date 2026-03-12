package cn.swiftadmin.common.core.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储策略接口
 * <p>定义文件存储的通用操作，支持多种存储实现（本地、OSS 等）</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface FileStorageStrategy {

    /**
     * 获取存储策略类型
     *
     * @return 存储类型标识（如 "local", "oss"）
     */
    String getStorageType();

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param path 存储路径（可选）
     * @return 文件信息
     * @throws FileStorageException 文件存储异常
     */
    FileInfo upload(MultipartFile file, String path) throws FileStorageException;

    /**
     * 上传文件（使用默认路径）
     *
     * @param file 上传的文件
     * @return 文件信息
     * @throws FileStorageException 文件存储异常
     */
    default FileInfo upload(MultipartFile file) throws FileStorageException {
        return upload(file, null);
    }

    /**
     * 删除文件
     *
     * @param fileId 文件 ID
     * @throws FileStorageException 文件存储异常
     */
    void delete(String fileId) throws FileStorageException;

    /**
     * 检查文件是否存在
     *
     * @param fileId 文件 ID
     * @return 是否存在
     * @throws FileStorageException 文件存储异常
     */
    boolean exists(String fileId) throws FileStorageException;

    /**
     * 获取文件访问 URL
     *
     * @param fileId 文件 ID
     * @return 文件访问 URL
     * @throws FileStorageException 文件存储异常
     */
    String getUrl(String fileId) throws FileStorageException;

    /**
     * 获取文件字节数组
     *
     * @param fileId 文件 ID
     * @return 文件字节数组
     * @throws FileStorageException 文件存储异常
     */
    byte[] getBytes(String fileId) throws FileStorageException;
}
