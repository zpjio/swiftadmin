package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.system.api.entity.SysOperLog;
import cn.swiftadmin.system.api.service.OperLogService;
import cn.swiftadmin.system.mapper.SysOperLogMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static cn.swiftadmin.system.api.entity.table.SysOperLogTableDef.SYS_OPER_LOG;

/**
 * 操作日志服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Service
public class OperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements OperLogService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Async
    public void insertLog(SysOperLog operLog) {
        save(operLog);
    }

    @Override
    public Page<SysOperLog> page(String title, String operName, Integer businessType,
                                  Integer status, String beginTime, String endTime,
                                  Integer page, Integer pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_OPER_LOG.TITLE.like(PageHelper.likeIfPresent(title)))
                .and(SYS_OPER_LOG.OPER_NAME.like(PageHelper.likeIfPresent(operName)))
                .and(SYS_OPER_LOG.BUSINESS_TYPE.eq(businessType, businessType != null))
                .and(SYS_OPER_LOG.STATUS.eq(status, status != null))
                .and(SYS_OPER_LOG.OPER_TIME.ge(parseTime(beginTime), beginTime != null && !beginTime.isEmpty()))
                .and(SYS_OPER_LOG.OPER_TIME.le(parseTime(endTime), endTime != null && !endTime.isEmpty()))
                .orderBy(SYS_OPER_LOG.OPER_TIME.desc());

        return PageHelper.convert(
                getMapper().paginate(PageHelper.of(page, pageSize), queryWrapper),
                log -> log
        );
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            getMapper().deleteBatchByIds(ids);
        }
    }

    @Override
    public void clearAll() {
        getMapper().deleteByQuery(QueryWrapper.create());
    }

    private LocalDateTime parseTime(String time) {
        if (time == null || time.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(time, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

}