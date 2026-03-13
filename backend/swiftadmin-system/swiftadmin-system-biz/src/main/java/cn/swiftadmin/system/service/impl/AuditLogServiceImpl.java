package cn.swiftadmin.system.service.impl;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.system.api.dto.AuditLogQueryDTO;
import cn.swiftadmin.system.api.entity.SysAuditLog;
import cn.swiftadmin.system.api.service.AuditLogService;
import cn.swiftadmin.system.mapper.SysAuditLogMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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

    @Override
    public Page<SysAuditLog> page(AuditLogQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_AUDIT_LOG.MODULE.eq(query.getModule(), StringUtils.hasText(query.getModule())))
                .and(SYS_AUDIT_LOG.OPERATION.eq(query.getOperation(), StringUtils.hasText(query.getOperation())))
                .and(SYS_AUDIT_LOG.OPER_USER_NAME.like(PageHelper.likeIfPresent(query.getOperUserName())))
                .and(SYS_AUDIT_LOG.TABLE_NAME.eq(query.getTableName(), StringUtils.hasText(query.getTableName())))
                .and(SYS_AUDIT_LOG.OPER_TIME.ge(query.getBeginTime(), query.getBeginTime() != null))
                .and(SYS_AUDIT_LOG.OPER_TIME.le(query.getEndTime(), query.getEndTime() != null))
                .orderBy(SYS_AUDIT_LOG.OPER_TIME.desc());

        return mapper.paginate(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper);
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
        if (!CollectionUtils.isEmpty(ids)) {
            mapper.deleteBatchByIds(ids);
        }
    }

    @Override
    public void clearBeforeDays(int beforeDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(beforeDays);
        mapper.deleteByQuery(QueryWrapper.create()
                .where(SYS_AUDIT_LOG.OPER_TIME.lt(threshold)));
        log.info("已清理 {} 天前的审计日志", beforeDays);
    }

    @Override
    public List<String> listModules() {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .select(distinct(SYS_AUDIT_LOG.MODULE)))
                .stream()
                .map(SysAuditLog::getModule)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

}