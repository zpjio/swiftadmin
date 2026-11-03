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
     * 允许上传的文件扩展名（逗号分隔）。
     * <p>默认是常见办公/图片/压缩包。设为空字符串表示不按白名单限制（仍受禁止列表约束）。</p>
     */
    private String allowedExtensions = "jpg,jpeg,png,gif,webp,bmp,pdf,txt,csv,doc,docx,xls,xlsx,ppt,pptx,zip,rar,7z";

    /**
     * 禁止上传的文件扩展名（逗号分隔）。优先级高于允许列表。
     * <p>包含可执行文件和可在浏览器中执行的页面/脚本，避免上传后被同源打开</p>
     */
    private String forbiddenExtensions = "exe,bat,cmd,com,scr,vbs,js,mjs,cjs,reg,sh,ps1,psm1,"
            + "html,htm,xhtml,shtml,svg,xml,xsl,jsp,jspx,php,asp,aspx,cer,cgi,"
            + "jar,war,ear,class,msi,dll,so,dylib,wasm,lnk";

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
