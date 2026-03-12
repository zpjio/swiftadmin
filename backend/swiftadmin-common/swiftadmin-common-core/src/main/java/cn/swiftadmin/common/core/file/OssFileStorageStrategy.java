package cn.swiftadmin.common.core.file;

import cn.swiftadmin.common.core.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * OSS 文件存储策略实现
 * <p>支持阿里云 OSS、腾讯云 COS、MinIO 等 S3 兼容存储</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Component
@ConditionalOnClass(S3Client.class)
@ConditionalOnProperty(prefix = "file.storage.oss", name = "enabled", havingValue = "true")
public class OssFileStorageStrategy implements FileStorageStrategy {

    @Value("${file.storage.oss.endpoint}")
    private String endpoint;

    @Value("${file.storage.oss.access-key-id}")
    private String accessKeyId;

    @Value("${file.storage.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${file.storage.oss.bucket-name}")
    private String bucketName;

    @Value("${file.storage.oss.region:cn-hangzhou}")
    private String region;

    @Value("${file.storage.oss.custom-domain:}")
    private String customDomain;

    @Value("${file.storage.oss.path-prefix:uploads}")
    private String pathPrefix;

    @Value("${file.storage.oss.max-file-size:10485760}")
    private long maxFileSize;

    private S3Client s3Client;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            this.s3Client = S3Client.builder()
                    .endpointOverride(java.net.URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, accessKeySecret)))
                    .build();
            
            if (!bucketExists()) {
                log.warn("OSS Bucket 不存在：{}", bucketName);
            }
            
            log.info("OSS 文件存储初始化完成，Bucket: {}", bucketName);
        } catch (Exception e) {
            log.error("初始化 OSS 文件存储失败", e);
            throw new FileStorageException("初始化 OSS 文件存储失败", e);
        }
    }

    private boolean bucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "oss";
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
            String objectKey = path != null ? String.format("%s/%s/%s", pathPrefix, path, fileName)
                    : String.format("%s/%s/%s", pathPrefix, datePath, fileName);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = getFileUrl(objectKey);

            return FileInfo.builder()
                    .fileId(objectKey)
                    .fileName(fileName)
                    .originalFileName(originalFileName)
                    .url(url)
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .storageType(getStorageType())
                    .path(objectKey)
                    .extension(extension)
                    .uploadTime(System.currentTimeMillis())
                    .build();

        } catch (IOException e) {
            log.error("文件上传失败：{}", file.getOriginalFilename(), e);
            throw new FileStorageException("文件上传失败：" + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("OSS 上传失败：{}", file.getOriginalFilename(), e);
            throw new FileStorageException("OSS 上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileId) throws FileStorageException {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build());
            log.info("删除 OSS 文件：{}", fileId);
        } catch (S3Exception e) {
            log.error("删除 OSS 文件失败：{}", fileId, e);
            throw new FileStorageException(ErrorCode.FILE_DELETE_FAILED, "删除文件失败", e);
        }
    }

    @Override
    public boolean exists(String fileId) throws FileStorageException {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("检查 OSS 文件存在失败：{}", fileId, e);
            throw new FileStorageException("检查文件存在失败", e);
        }
    }

    @Override
    public String getUrl(String fileId) throws FileStorageException {
        if (exists(fileId)) {
            return getFileUrl(fileId);
        }
        throw new FileStorageException(ErrorCode.FILE_NOT_FOUND, "文件不存在：" + fileId);
    }

    @Override
    public byte[] getBytes(String fileId) throws FileStorageException {
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build());
            return response.asByteArray();
        } catch (S3Exception e) {
            log.error("读取 OSS 文件失败：{}", fileId, e);
            throw new FileStorageException("读取文件失败", e);
        }
    }

    /**
     * 获取文件访问 URL
     * <p>阿里云 OSS 标准格式：https://{bucket}.{endpoint}/{objectKey}</p>
     */
    private String getFileUrl(String objectKey) {
        if (customDomain != null && !customDomain.isEmpty()) {
            return customDomain + "/" + objectKey;
        } else {
            // 移除协议前缀，生成标准 OSS URL：{bucket}.{endpoint}/{objectKey}
            String endpointHost = endpoint.replaceFirst("^https?://", "");
            return String.format("https://%s.%s/%s", bucketName, endpointHost, objectKey);
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
