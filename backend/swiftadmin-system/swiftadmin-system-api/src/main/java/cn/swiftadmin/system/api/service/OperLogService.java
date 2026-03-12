package cn.swiftadmin.system.api.service;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.system.api.dto.OperLogQueryDTO;
import cn.swiftadmin.system.api.entity.SysOperLog;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 操作日志服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface OperLogService extends IService<SysOperLog> {

    /**
     * 插入操作日志（异步）
     *
     * @param operLog 操作日志
     */
    void insertLog(SysOperLog operLog);

    /**
     * 分页查询操作日志
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<SysOperLog> page(OperLogQueryDTO query);

    /**
     * 批量删除操作日志
     *
     * @param ids 日志ID列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 清空操作日志
     */
    void clearAll();

}