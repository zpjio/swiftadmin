package cn.swiftadmin.common.core.file;

import cn.swiftadmin.common.core.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传服务
 * <p>统一管理文件上传操作，支持多种存储策略</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final List<FileStorageStrategy> storageStrategies;
    private final FileStorageProperties fileStorageProperties;
    private final Map<String, FileStorageStrategy> strategyMap = new HashMap<>();

    /** 禁止上传的扩展名集合 */
    private Set<String> forbiddenExtensions;

    /** 允许上传的扩展名集合（为空表示不限制） */
    private Set<String> allowedExtensions;

    @Value("${file.storage.type:local}")
    private String defaultStorageType;

    @jakarta.annotation.PostConstruct
    public void init() {
        for (FileStorageStrategy strategy : storageStrategies) {
            strategyMap.put(strategy.getStorageType(), strategy);
        }

        if (!strategyMap.containsKey(defaultStorageType)) {
            log.warn("默认存储策略 {} 不可用，使用本地存储", defaultStorageType);
            defaultStorageType = "local";
        }

        // 初始化文件扩展名限制
        initExtensionFilters();

        log.debug("文件上传服务初始化完成，存储策略：{}，默认：{}", strategyMap.keySet(), defaultStorageType);
    }

    /**
     * 初始化文件扩展名过滤配置
     */
    private void initExtensionFilters() {
        // 禁止的扩展名
        String forbidden = fileStorageProperties.getForbiddenExtensions();
        if (StringUtils.hasText(forbidden)) {
            forbiddenExtensions = new HashSet<>(
                    Arrays.asList(forbidden.toLowerCase().split(","))
            );
        } else {
            // 默认禁止的可执行文件类型
            forbiddenExtensions = new HashSet<>(Arrays.asList(
                    "exe", "bat", "sh", "cmd", "com", "scr", "vbs", "js", "reg"
            ));
        }

        // 允许的扩展名
        String allowed = fileStorageProperties.getAllowedExtensions();
        if (StringUtils.hasText(allowed)) {
            allowedExtensions = new HashSet<>(
                    Arrays.asList(allowed.toLowerCase().split(","))
            );
        }
    }

    /**
     * 上传文件（使用默认存储策略）
     *
     * @param file 上传的文件
     * @return 文件信息
     * @throws FileStorageException 文件存储异常
     */
    public FileInfo upload(MultipartFile file) throws FileStorageException {
        return upload(file, null, null);
    }

    /**
     * 上传文件（使用默认存储策略）
     *
     * @param file 上传的文件
     * @param path 存储路径
     * @return 文件信息
     * @throws FileStorageException 文件存储异常
     */
    public FileInfo upload(MultipartFile file, String path) throws FileStorageException {
        return upload(file, path, null);
    }

    /**
     * 上传文件（指定存储策略）
     *
     * @param file 上传的文件
     * @param path 存储路径
     * @param storageType 存储类型（local/oss）
     * @return 文件信息
     * @throws FileStorageException 文件存储异常
     */
    public FileInfo upload(MultipartFile file, String path, String storageType) throws FileStorageException {
        // 校验文件
        validateFile(file);

        String type = storageType != null ? storageType : defaultStorageType;
        FileStorageStrategy strategy = getStrategy(type);

        log.info("上传文件：{} ({} bytes) 到 {}",
                file.getOriginalFilename(), file.getSize(), type);

        return strategy.upload(file, path);
    }

    /**
     * 校验上传文件
     *
     * @param file 上传的文件
     * @throws FileStorageException 校验失败
     */
    private void validateFile(MultipartFile file) throws FileStorageException {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasLength(originalFilename)) {
            throw new FileStorageException("文件名不能为空");
        }

        // 获取文件扩展名
        String extension = getFileExtension(originalFilename);
        if (!StringUtils.hasLength(extension)) {
            throw new FileStorageException("无法识别文件类型");
        }

        String extLower = extension.toLowerCase();

        // 检查是否在禁止列表中
        if (forbiddenExtensions != null && forbiddenExtensions.contains(extLower)) {
            throw new FileStorageException("不允许上传该类型的文件：" + extension);
        }

        // 检查是否在允许列表中（如果配置了允许列表）
        if (!CollectionUtils.isEmpty(allowedExtensions)) {
            if (!allowedExtensions.contains(extLower)) {
                throw new FileStorageException("不允许上传该类型的文件：" + extension);
            }
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (!StringUtils.hasLength(fileName)) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * 删除文件
     *
     * @param fileId 文件 ID
     * @param storageType 存储类型
     * @throws FileStorageException 文件存储异常
     */
    public void delete(String fileId, String storageType) throws FileStorageException {
        String type = storageType != null ? storageType : defaultStorageType;
        FileStorageStrategy strategy = getStrategy(type);
        strategy.delete(fileId);
    }

    /**
     * 删除文件（使用默认存储策略）
     *
     * @param fileId 文件 ID
     * @throws FileStorageException 文件存储异常
     */
    public void delete(String fileId) throws FileStorageException {
        delete(fileId, null);
    }

    /**
     * 获取文件访问 URL
     *
     * @param fileId 文件 ID
     * @param storageType 存储类型
     * @return 文件访问 URL
     * @throws FileStorageException 文件存储异常
     */
    public String getUrl(String fileId, String storageType) throws FileStorageException {
        String type = storageType != null ? storageType : defaultStorageType;
        FileStorageStrategy strategy = getStrategy(type);
        return strategy.getUrl(fileId);
    }

    /**
     * 获取文件访问 URL（使用默认存储策略）
     *
     * @param fileId 文件 ID
     * @return 文件访问 URL
     * @throws FileStorageException 文件存储异常
     */
    public String getUrl(String fileId) throws FileStorageException {
        return getUrl(fileId, null);
    }

    /**
     * 获取文件字节数组
     *
     * @param fileId 文件 ID
     * @param storageType 存储类型
     * @return 文件字节数组
     * @throws FileStorageException 文件存储异常
     */
    public byte[] getBytes(String fileId, String storageType) throws FileStorageException {
        String type = storageType != null ? storageType : defaultStorageType;
        FileStorageStrategy strategy = getStrategy(type);
        return strategy.getBytes(fileId);
    }

    /**
     * 获取文件字节数组（使用默认存储策略）
     *
     * @param fileId 文件 ID
     * @return 文件字节数组
     * @throws FileStorageException 文件存储异常
     */
    public byte[] getBytes(String fileId) throws FileStorageException {
        return getBytes(fileId, null);
    }

    /**
     * 检查文件是否存在
     *
     * @param fileId 文件 ID
     * @param storageType 存储类型
     * @return 是否存在
     * @throws FileStorageException 文件存储异常
     */
    public boolean exists(String fileId, String storageType) throws FileStorageException {
        String type = storageType != null ? storageType : defaultStorageType;
        FileStorageStrategy strategy = getStrategy(type);
        return strategy.exists(fileId);
    }

    /**
     * 检查文件是否存在（使用默认存储策略）
     *
     * @param fileId 文件 ID
     * @return 是否存在
     * @throws FileStorageException 文件存储异常
     */
    public boolean exists(String fileId) throws FileStorageException {
        return exists(fileId, null);
    }

    /**
     * 获取存储策略
     *
     * @param storageType 存储类型
     * @return 存储策略
     * @throws FileStorageException 存储策略不存在
     */
    private FileStorageStrategy getStrategy(String storageType) {
        FileStorageStrategy strategy = strategyMap.get(storageType);
        if (strategy == null) {
            throw new FileStorageException("不支持的存储类型：" + storageType);
        }
        return strategy;
    }

    /**
     * 获取所有可用的存储类型
     *
     * @return 存储类型列表
     */
    public List<String> getAvailableStorageTypes() {
        return storageStrategies.stream()
                .map(FileStorageStrategy::getStorageType)
                .toList();
    }
}
