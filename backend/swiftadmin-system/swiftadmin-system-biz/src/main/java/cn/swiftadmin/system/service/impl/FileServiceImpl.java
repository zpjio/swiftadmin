package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.file.FileInfo;
import cn.swiftadmin.common.core.file.FileStorageException;
import cn.swiftadmin.common.core.file.FileUploadService;
import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.common.core.util.BeanCopyUtils;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.common.security.util.SecurityUtils;
import cn.swiftadmin.system.api.entity.SysFile;
import cn.swiftadmin.system.api.service.FileService;
import cn.swiftadmin.system.api.vo.FileVO;
import cn.swiftadmin.system.mapper.SysFileMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysFileTableDef.SYS_FILE;

/**
 * 文件服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements FileService {

    private final FileUploadService fileUploadService;

    @Override
    public Page<FileVO> page(String fileName, String bizType, String storageType, Integer status, Integer page, Integer pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_FILE.FILE_NAME.like(PageHelper.likeIfPresent(fileName)))
                .and(SYS_FILE.BIZ_TYPE.eq(bizType, bizType != null && !bizType.isEmpty()))
                .and(SYS_FILE.STORAGE_TYPE.eq(storageType, storageType != null && !storageType.isEmpty()))
                .and(SYS_FILE.STATUS.eq(status, status != null))
                .orderBy(SYS_FILE.CREATE_TIME.desc());

        return PageHelper.convert(
                getMapper().paginate(PageHelper.of(page, pageSize), queryWrapper),
                file -> toFileVO(file)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileVO upload(MultipartFile file, String path, String storageType, String bizType) {
        try {
            // 调用底层存储服务上传文件
            FileInfo fileInfo = fileUploadService.upload(file, path, storageType);

            // 获取当前上传用户信息
            Long uploadUserId = SecurityUtils.getCurrentUserId();
            String uploadUserName = SecurityUtils.getCurrentUsername();

            // 保存文件记录到数据库
            SysFile sysFile = SysFile.builder()
                    .fileName(file.getOriginalFilename())
                    .storageName(fileInfo.getFileId())
                    .filePath(fileInfo.getPath())
                    .fileSize(file.getSize())
                    .fileType(fileInfo.getContentType())
                    .fileExtension(fileInfo.getExtension())
                    .storageType(fileInfo.getStorageType())
                    .accessUrl(fileInfo.getUrl())
                    .bizType(bizType)
                    .uploadUserId(uploadUserId)
                    .uploadUserName(uploadUserName)
                    .downloadCount(0)
                    .status(1)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .deleted(0)
                    .build();

            save(sysFile);

            log.info("文件上传成功：id={}, fileName={}, uploadUser={}",
                    sysFile.getId(), sysFile.getFileName(), uploadUserName);
            return toFileVO(sysFile);
        } catch (FileStorageException e) {
            log.error("文件上传失败：{}", e.getMessage());
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FileVO> uploadBatch(List<MultipartFile> files, String path, String storageType, String bizType) {
        return files.stream()
                .map(file -> {
                    try {
                        return upload(file, path, storageType, bizType);
                    } catch (Exception e) {
                        log.error("批量上传文件失败：{}", file.getOriginalFilename(), e);
                        return null;
                    }
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());
    }

    @Override
    public FileVO getDetail(Long id) {
        SysFile file = getById(id);
        return file != null ? toFileVO(file) : null;
    }

    @Override
    public String getAccessUrl(Long id) {
        SysFile file = getById(id);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        return file.getAccessUrl();
    }

    @Override
    public byte[] download(Long id) {
        SysFile file = getById(id);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }

        try {
            byte[] bytes = fileUploadService.getBytes(file.getStorageName(), file.getStorageType());

            // 更新下载次数
            getMapper().updateByQuery(
                    SysFile.builder().downloadCount(file.getDownloadCount() + 1).build(),
                    QueryWrapper.create().where(SYS_FILE.ID.eq(id))
            );

            return bytes;
        } catch (FileStorageException e) {
            log.error("文件下载失败：id={}", id, e);
            throw new RuntimeException("文件下载失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFiles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Long id : ids) {
            SysFile file = getById(id);
            if (file != null) {
                try {
                    // 删除物理文件
                    fileUploadService.delete(file.getStorageName(), file.getStorageType());
                    // 删除数据库记录
                    removeById(id);
                    log.info("文件删除成功：id={}", id);
                } catch (FileStorageException e) {
                    log.warn("物理文件删除失败，仅删除数据库记录：id={}", id);
                    removeById(id);
                }
            }
        }
    }

    @Override
    public List<String> getAvailableStorageTypes() {
        return fileUploadService.getAvailableStorageTypes();
    }

    /**
     * 转换为 VO（使用带后置处理器的拷贝）
     */
    private FileVO toFileVO(SysFile file) {
        return BeanCopyUtils.copy(file, FileVO::new, (src, vo) -> {
            if (src.getFileSize() != null) {
                vo.setFileSizeFormat(formatFileSize(src.getFileSize()));
            }
        });
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null || size < 0) {
            return "0 B";
        }
        if (size < 1024) {
            return size + " B";
        }
        double kb = size / 1024.0;
        if (kb < 1024) {
            return String.format("%.2f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.2f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }
}