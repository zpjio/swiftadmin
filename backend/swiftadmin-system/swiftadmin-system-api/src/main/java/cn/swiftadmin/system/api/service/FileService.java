package cn.swiftadmin.system.api.service;

import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.system.api.entity.SysFile;
import cn.swiftadmin.system.api.vo.FileVO;
import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface FileService extends IService<SysFile> {

    /**
     * 分页查询文件列表
     *
     * @param fileName 文件名
     * @param bizType 业务类型
     * @param storageType 存储类型
     * @param status 状态
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<FileVO> page(String fileName, String bizType, String storageType, Integer status, Integer page, Integer pageSize);

    /**
     * 上传单个文件
     *
     * @param file 文件
     * @param path 存储路径
     * @param storageType 存储类型
     * @param bizType 业务类型
     * @return 文件信息
     */
    FileVO upload(MultipartFile file, String path, String storageType, String bizType);

    /**
     * 批量上传文件
     *
     * @param files 文件列表
     * @param path 存储路径
     * @param storageType 存储类型
     * @param bizType 业务类型
     * @return 文件信息列表
     */
    List<FileVO> uploadBatch(List<MultipartFile> files, String path, String storageType, String bizType);

    /**
     * 获取文件详情
     *
     * @param id 文件ID
     * @return 文件信息
     */
    FileVO getDetail(Long id);

    /**
     * 获取文件访问URL
     *
     * @param id 文件ID
     * @return 访问URL
     */
    String getAccessUrl(Long id);

    /**
     * 下载文件
     *
     * @param id 文件ID
     * @return 文件字节数组
     */
    byte[] download(Long id);

    /**
     * 删除文件
     *
     * @param ids 文件ID列表
     */
    void deleteFiles(List<Long> ids);

    /**
     * 获取可用的存储类型
     *
     * @return 存储类型列表
     */
    List<String> getAvailableStorageTypes();
}