package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.common.core.result.Page;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.system.api.entity.SysAuditLog;
import cn.swiftadmin.system.api.service.AuditLogService;
import cn.swiftadmin.system.mapper.SysAuditLogMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static cn.swiftadmin.system.api.entity.table.SysAuditLogTableDef.SYS_AUDIT_LOG;
import static com.mybatisflex.core.query.QueryMethods.distinct;

/**
 * 审计日志服务实现类
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Service
public class AuditLogServiceImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog> implements AuditLogService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<SysAuditLog> page(String module, String operation, String operUserName,
                                   String tableName, String beginTime, String endTime,
                                   Integer page, Integer pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_AUDIT_LOG.MODULE.eq(module, module != null && !module.isEmpty()))
                .and(SYS_AUDIT_LOG.OPERATION.eq(operation, operation != null && !operation.isEmpty()))
                .and(SYS_AUDIT_LOG.OPER_USER_NAME.like(PageHelper.likeIfPresent(operUserName)))
                .and(SYS_AUDIT_LOG.TABLE_NAME.eq(tableName, tableName != null && !tableName.isEmpty()))
                .and(SYS_AUDIT_LOG.OPER_TIME.ge(parseTime(beginTime), beginTime != null && !beginTime.isEmpty()))
                .and(SYS_AUDIT_LOG.OPER_TIME.le(parseTime(endTime), endTime != null && !endTime.isEmpty()))
                .orderBy(SYS_AUDIT_LOG.OPER_TIME.desc());

        return PageHelper.convert(
                getMapper().paginate(PageHelper.of(page, pageSize), queryWrapper),
                log -> log
        );
    }

    @Override
    public SysAuditLog getDetail(Long id) {
        return getById(id);
    }

    @Override
    public List<SysAuditLog> getRecordHistory(String tableName, String recordId) {
        return list(QueryWrapper.create()
                .where(SYS_AUDIT_LOG.TABLE_NAME.eq(tableName))
                .and(SYS_AUDIT_LOG.RECORD_ID.eq(recordId))
                .orderBy(SYS_AUDIT_LOG.OPER_TIME.desc()));
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            getMapper().deleteBatchByIds(ids);
        }
    }

    @Override
    public void clearBeforeDays(int beforeDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(beforeDays);
        getMapper().deleteByQuery(QueryWrapper.create()
                .where(SYS_AUDIT_LOG.OPER_TIME.lt(threshold)));
        log.info("已清理 {} 天前的审计日志", beforeDays);
    }

    @Override
    public List<String> listModules() {
        return getMapper().selectListByQuery(
                        QueryWrapper.create()
                                .select(distinct(SYS_AUDIT_LOG.MODULE)))
                .stream()
                .map(SysAuditLog::getModule)
                .filter(m -> m != null && !m.isEmpty())
                .collect(Collectors.toList());
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