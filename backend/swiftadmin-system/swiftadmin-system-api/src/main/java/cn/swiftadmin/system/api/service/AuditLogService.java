package cn.swiftadmin.system.api.service;

import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.system.api.entity.SysAuditLog;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 审计日志服务接口
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public interface AuditLogService extends IService<SysAuditLog> {

    /**
     * 分页查询审计日志
     *
     * @param module 模块名称
     * @param operation 操作类型
     * @param operUserName 操作用户名
     * @param tableName 表名
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<SysAuditLog> page(String module, String operation, String operUserName,
                           String tableName, String beginTime, String endTime,
                           Integer page, Integer pageSize);

    /**
     * 获取审计日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    SysAuditLog getDetail(Long id);

    /**
     * 获取指定记录的变更历史
     *
     * @param tableName 表名
     * @param recordId 记录ID
     * @return 变更历史列表
     */
    List<SysAuditLog> getRecordHistory(String tableName, String recordId);

    /**
     * 批量删除审计日志
     *
     * @param ids 日志ID列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 清空指定天数前的审计日志
     *
     * @param beforeDays 天数
     */
    void clearBeforeDays(int beforeDays);

    /**
     * 获取模块列表
     *
     * @return 模块列表
     */
    List<String> listModules();

}