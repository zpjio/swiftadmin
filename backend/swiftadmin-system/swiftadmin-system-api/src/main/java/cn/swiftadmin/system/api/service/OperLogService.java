package cn.swiftadmin.system.api.service;

import cn.swiftadmin.common.core.result.Page;
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
     * @param title 操作标题
     * @param operName 操作人员
     * @param businessType 业务类型
     * @param status 操作状态
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<SysOperLog> page(String title, String operName, Integer businessType,
                          Integer status, String beginTime, String endTime,
                          Integer page, Integer pageSize);

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