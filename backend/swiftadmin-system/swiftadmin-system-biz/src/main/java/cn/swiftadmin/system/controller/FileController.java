package cn.swiftadmin.system.controller;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.security.annotation.OperationLog;
import cn.swiftadmin.common.security.annotation.RequiresPermission;
import cn.swiftadmin.system.api.dto.FileQueryDTO;
import cn.swiftadmin.system.api.service.FileService;
import cn.swiftadmin.system.api.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件管理控制器
 * <p>提供文件上传、下载、删除等管理功能</p>
 * <p>支持多种存储方式：本地、OSS、MinIO 等</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Tag(name = "文件管理", description = "文件上传、下载、删除等管理接口")
@RestController
@RequestMapping("/api/system/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 分页查询文件列表
     */
    @Operation(summary = "分页查询文件列表", description = "支持按文件名、业务类型、存储类型、状态筛选")
    @GetMapping("/page")
    @RequiresPermission("system:file:list")
    public R<Page<FileVO>> page(FileQueryDTO query) {
        return R.ok(fileService.page(query));
    }

    /**
     * 上传单个文件
     * <p>支持多种存储类型，可指定存储路径和业务类型</p>
     *
     * @param file 上传的文件
     * @param path 存储路径（可选，默认按日期分类）
     * @param storageType 存储类型（可选，默认使用系统配置）
     * @param bizType 业务类型（用于分类管理）
     * @return 文件信息（包含文件 ID、访问 URL 等）
     */
    @Operation(summary = "上传单个文件", description = "支持多种存储类型，可指定存储路径和业务类型")
    @PostMapping("/upload")
    @RequiresPermission("system:file:upload")
    @OperationLog(title = "文件管理", businessType = OperationLog.BusinessType.INSERT)
    public R<FileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "storageType", required = false) String storageType,
            @RequestParam(value = "bizType", required = false) String bizType) {
        FileVO fileVO = fileService.upload(file, path, storageType, bizType);
        return R.ok(fileVO);
    }

    /**
     * 批量上传文件
     * <p>一次性上传多个文件，返回文件信息列表</p>
     *
     * @param files 上传的文件列表
     * @param path 存储路径（可选）
     * @param storageType 存储类型（可选）
     * @param bizType 业务类型（可选）
     * @return 文件信息列表
     */
    @Operation(summary = "批量上传文件", description = "一次性上传多个文件，返回文件信息列表")
    @PostMapping("/upload/batch")
    @RequiresPermission("system:file:upload")
    @OperationLog(title = "文件管理", businessType = OperationLog.BusinessType.INSERT)
    public R<List<FileVO>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "storageType", required = false) String storageType,
            @RequestParam(value = "bizType", required = false) String bizType) {
        List<FileVO> fileVOs = fileService.uploadBatch(files, path, storageType, bizType);
        return R.ok(fileVOs);
    }

    /**
     * 获取文件详情
     * <p>根据文件 ID 获取文件信息</p>
     *
     * @param id 文件 ID
     * @return 文件详情，不存在时返回 404
     */
    @Operation(summary = "获取文件详情", description = "根据文件 ID 获取文件信息")
    @GetMapping("/{id}")
    @RequiresPermission("system:file:query")
    public R<FileVO> getDetail(@PathVariable Long id) {
        FileVO fileVO = fileService.getDetail(id);
        return fileVO != null ? R.ok(fileVO) : R.fail(404, "文件不存在");
    }

    /**
     * 获取文件访问 URL
     * <p>返回文件的访问链接，用于前端展示</p>
     *
     * @param id 文件 ID
     * @return 文件访问 URL
     */
    @Operation(summary = "获取文件访问 URL", description = "返回文件的访问链接，用于前端展示")
    @GetMapping("/url/{id}")
    @RequiresPermission("system:file:query")
    public R<String> getFileUrl(@PathVariable Long id) {
        String url = fileService.getAccessUrl(id);
        return R.ok(url);
    }

    /**
     * 下载文件
     * <p>返回文件二进制数据，用于文件下载</p>
     *
     * @param id 文件 ID
     * @return 文件二进制数据
     */
    @Operation(summary = "下载文件", description = "返回文件二进制数据，用于文件下载")
    @GetMapping("/download/{id}")
    @RequiresPermission("system:file:download")
    @OperationLog(title = "文件管理", businessType = OperationLog.BusinessType.EXPORT)
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        // 下载文件内容
        byte[] bytes = fileService.download(id);

        // 获取文件信息
        FileVO fileVO = fileService.getDetail(id);
        if (fileVO == null) {
            return ResponseEntity.notFound().build();
        }

        // 处理文件名编码，支持中文
        String encodedFileName = URLEncoder.encode(fileVO.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        // 根据文件类型设置 Content-Type
        String contentType = fileVO.getFileType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .body(bytes);
    }

    /**
     * 批量删除文件
     * <p>支持同时删除多个文件，包括存储中的物理文件</p>
     *
     * @param ids 文件 ID 列表
     * @return 操作结果
     */
    @Operation(summary = "批量删除文件", description = "支持同时删除多个文件，包括存储中的物理文件")
    @DeleteMapping("/{ids}")
    @RequiresPermission("system:file:delete")
    @OperationLog(title = "文件管理", businessType = OperationLog.BusinessType.DELETE)
    public R<Void> deleteFiles(@PathVariable List<Long> ids) {
        fileService.deleteFiles(ids);
        return R.ok();
    }

    /**
     * 获取可用的存储类型列表
     * <p>返回系统配置的所有可用存储方式</p>
     *
     * @return 存储类型列表（如：local, oss, minio 等）
     */
    @Operation(summary = "获取可用存储类型", description = "返回系统配置的所有可用存储方式")
    @GetMapping("/storage-types")
    public R<List<String>> getAvailableStorageTypes() {
        return R.ok(fileService.getAvailableStorageTypes());
    }
}