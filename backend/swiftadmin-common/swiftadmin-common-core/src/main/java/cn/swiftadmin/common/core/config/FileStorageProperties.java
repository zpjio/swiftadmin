package cn.swiftadmin.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置
 * <p>支持本地存储和 OSS 存储配置</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /**
     * 默认存储类型：local, oss
     */
    private String type = "local";

    /**
     * 允许上传的文件扩展名（逗号分隔，如 "jpg,jpeg,png,gif,pdf,doc,docx"）
     * 为空表示不限制
     */
    private String allowedExtensions;

    /**
     * 禁止上传的文件扩展名（逗号分隔，如 "exe,bat,sh,cmd"）
     * 优先级高于允许列表
     */
    private String forbiddenExtensions = "exe,bat,sh,cmd,com,scr,vbs,js,reg,bat,cmd";

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * OSS 存储配置
     */
    private Oss oss = new Oss();

    @Data
    public static class Local {
        /**
         * 本地存储基础路径
         */
        private String basePath = "./uploads";

        /**
         * 文件访问 URL 前缀
         */
        private String accessUrl = "/files";

        /**
         * 单个文件最大大小（字节），默认 10MB
         */
        private Long maxFileSize = 10485760L;
    }

    @Data
    public static class Oss {
        /**
         * 是否启用 OSS 存储
         */
        private Boolean enabled = false;

        /**
         * OSS 端点地址
         */
        private String endpoint;

        /**
         * 访问密钥 ID
         */
        private String accessKeyId;

        /**
         * 访问密钥 Secret
         */
        private String accessKeySecret;

        /**
         * Bucket 名称
         */
        private String bucketName;

        /**
         * 区域
         */
        private String region = "cn-hangzhou";

        /**
         * 自定义域名（可选）
         */
        private String customDomain;

        /**
         * 路径前缀
         */
        private String pathPrefix = "uploads";

        /**
         * 单个文件最大大小（字节），默认 10MB
         */
        private Long maxFileSize = 10485760L;
    }
}
