package cn.swiftadmin.auth.service.impl;

import cn.swiftadmin.auth.api.entity.SysLoginLog;
import cn.swiftadmin.auth.api.service.LoginLogService;
import cn.swiftadmin.auth.mapper.SysLoginLogMapper;
import cn.swiftadmin.common.core.enums.LogStatus;
import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static cn.swiftadmin.auth.api.entity.table.SysLoginLogTableDef.SYS_LOGIN_LOG;

/**
 * 登录日志服务实现类
 * <p>异步记录登录日志，避免阻塞登录流程</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Slf4j
@Service
public class LoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements LoginLogService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 异步记录登录日志
     */
    @Override
    @Async
    public void record(String username, String ipAddress, String userAgent, int status, String message) {
        try {
            SysLoginLog logEntry = SysLoginLog.builder()
                    .username(username)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(LogStatus.fromCode(status))
                    .message(message)
                    .loginTime(LocalDateTime.now())
                    .build();

            save(logEntry);
            log.debug("登录日志记录成功：{}", username);
        } catch (Exception e) {
            log.error("登录日志记录失败：{}", username, e);
        }
    }

    @Override
    public Page<SysLoginLog> page(String username, String ip, Integer status, String beginTime, String endTime, Integer page, Integer pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        if (username != null && !username.isEmpty()) {
            queryWrapper.and(SYS_LOGIN_LOG.USERNAME.like(username));
        }
        if (ip != null && !ip.isEmpty()) {
            queryWrapper.and(SYS_LOGIN_LOG.IP_ADDRESS.like(ip));
        }
        if (status != null) {
            queryWrapper.and(SYS_LOGIN_LOG.STATUS.eq(status));
        }
        if (beginTime != null && !beginTime.isEmpty()) {
            queryWrapper.and(SYS_LOGIN_LOG.LOGIN_TIME.ge(LocalDateTime.parse(beginTime + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }
        if (endTime != null && !endTime.isEmpty()) {
            queryWrapper.and(SYS_LOGIN_LOG.LOGIN_TIME.le(LocalDateTime.parse(endTime + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }
        queryWrapper.orderBy(SYS_LOGIN_LOG.LOGIN_TIME.desc());

        com.mybatisflex.core.paginate.Page<SysLoginLog> flexPage = getMapper().paginate(page, pageSize, queryWrapper);
        return PageHelper.convert(flexPage);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        removeByIds(ids);
    }

    @Override
    public void clearAll() {
        getMapper().deleteByQuery(QueryWrapper.create());
    }

}
