package cn.swiftadmin.auth.api.service;

import cn.swiftadmin.auth.api.entity.SysLoginLog;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 登录日志服务接口
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
public interface LoginLogService extends IService<SysLoginLog> {

    /**
     * 记录登录日志（异步）
     *
     * @param username  用户名
     * @param ipAddress IP 地址
     * @param userAgent 用户代理
     * @param status    状态：1=成功，0=失败
     * @param message   日志消息
     */
    void record(String username, String ipAddress, String userAgent, int status, String message);

    /**
     * 分页查询登录日志
     *
     * @param username  用户名
     * @param ip        IP 地址
     * @param status    状态
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @param page      页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<SysLoginLog> page(String username, String ip, Integer status, String beginTime, String endTime, Integer page, Integer pageSize);

    /**
     * 批量删除登录日志
     *
     * @param ids ID 列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 清空所有登录日志
     */
    void clearAll();

}
