package cn.swiftadmin.common.core.file;

import cn.swiftadmin.common.core.result.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储策略实现
 * <p>将文件存储到本地服务器文件系统</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
public class LocalFileStorageStrategy implements FileStorageStrategy {

    @Value("${file.storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${file.storage.local.access-url:/files}")
    private String accessUrl;

    @Value("${file.storage.local.max-file-size:10485760}")
    private long maxFileSize;

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String springMaxFileSize;

    @PostConstruct
    public void init() {
        try {
            Path baseDir = Paths.get(basePath);
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
                log.info("创建本地文件存储目录：{}", basePath);
            }
            log.debug("本地文件存储初始化完成，存储路径：{}", basePath);
        } catch (IOException e) {
            log.error("初始化本地文件存储目录失败", e);
            throw new FileStorageException("初始化本地文件存储目录失败", e);
        }
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    @Override
    public FileInfo upload(MultipartFile file, String path) throws FileStorageException {
        if (file.isEmpty()) {
            throw new FileStorageException("上传文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new FileStorageException(ErrorCode.FILE_SIZE_EXCEEDED, 
                "文件大小超出限制：" + formatFileSize(maxFileSize));
        }

        try {
            String originalFileName = file.getOriginalFilename();
            String extension = getFileExtension(originalFileName);
            String fileName = generateFileName(extension);
            
            String datePath = getDatePath();
            String savePath = path != null ? Paths.get(basePath, path).toString() 
                                           : Paths.get(basePath, datePath).toString();
            
            Path saveDir = Paths.get(savePath);
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
            }

            Path filePath = Paths.get(savePath, fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativePath = path != null ? Paths.get(path, fileName).toString()
                                               : Paths.get(datePath, fileName).toString();
            String url = accessUrl + "/" + relativePath.replace("\\", "/");

            return FileInfo.builder()
                    .fileId(fileName)
                    .fileName(fileName)
                    .originalFileName(originalFileName)
                    .url(url)
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .storageType(getStorageType())
                    .path(relativePath)
                    .extension(extension)
                    .uploadTime(System.currentTimeMillis())
                    .build();

        } catch (IOException e) {
            log.error("文件上传失败：{}", file.getOriginalFilename(), e);
            throw new FileStorageException("文件上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileId) throws FileStorageException {
        try {
            Path filePath = Paths.get(basePath, fileId);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("删除本地文件：{}", fileId);
            } else {
                log.warn("文件不存在，无法删除：{}", fileId);
                throw new FileStorageException(ErrorCode.FILE_NOT_FOUND, "文件不存在：" + fileId);
            }
        } catch (IOException e) {
            log.error("删除文件失败：{}", fileId, e);
            throw new FileStorageException(ErrorCode.FILE_DELETE_FAILED, "删除文件失败", e);
        }
    }

    @Override
    public boolean exists(String fileId) throws FileStorageException {
        try {
            Path filePath = Paths.get(basePath, fileId);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.error("检查文件存在失败：{}", fileId, e);
            throw new FileStorageException("检查文件存在失败", e);
        }
    }

    @Override
    public String getUrl(String fileId) throws FileStorageException {
        if (exists(fileId)) {
            return accessUrl + "/" + fileId.replace("\\", "/");
        }
        throw new FileStorageException(ErrorCode.FILE_NOT_FOUND, "文件不存在：" + fileId);
    }

    @Override
    public byte[] getBytes(String fileId) throws FileStorageException {
        try {
            Path filePath = Paths.get(basePath, fileId);
            if (!Files.exists(filePath)) {
                throw new FileStorageException(ErrorCode.FILE_NOT_FOUND, "文件不存在：" + fileId);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取文件失败：{}", fileId, e);
            throw new FileStorageException("读取文件失败", e);
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String extension) {
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex).toLowerCase();
    }

    /**
     * 获取日期路径（格式：yyyy/MM/dd）
     */
    private String getDatePath() {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        return now.format(formatter);
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2fGB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
