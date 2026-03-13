package cn.swiftadmin.auth.service.impl;

import cn.swiftadmin.auth.api.entity.SysLoginLog;
import cn.swiftadmin.auth.api.service.LoginLogService;
import cn.swiftadmin.auth.mapper.SysLoginLogMapper;
import cn.swiftadmin.common.core.enums.LogStatus;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

        if (StringUtils.hasText(username)) {
            queryWrapper.and(SYS_LOGIN_LOG.USERNAME.like(username));
        }
        if (StringUtils.hasText(ip)) {
            queryWrapper.and(SYS_LOGIN_LOG.IP_ADDRESS.like(ip));
        }
        if (status != null) {
            queryWrapper.and(SYS_LOGIN_LOG.STATUS.eq(status));
        }
        if (StringUtils.hasText(beginTime)) {
            queryWrapper.and(SYS_LOGIN_LOG.LOGIN_TIME.ge(LocalDateTime.parse(beginTime + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }
        if (StringUtils.hasText(endTime)) {
            queryWrapper.and(SYS_LOGIN_LOG.LOGIN_TIME.le(LocalDateTime.parse(endTime + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }
        queryWrapper.orderBy(SYS_LOGIN_LOG.LOGIN_TIME.desc());

        return mapper.paginate(page, pageSize, queryWrapper);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        removeByIds(ids);
    }

    @Override
    public void clearAll() {
        mapper.deleteByQuery(QueryWrapper.create());
    }

}
