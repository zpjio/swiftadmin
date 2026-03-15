package cn.swiftadmin.system.service.impl;

import com.mybatisflex.core.paginate.Page;
import cn.swiftadmin.common.mybatis.util.PageHelper;
import cn.swiftadmin.system.api.dto.OperLogQueryDTO;
import cn.swiftadmin.system.api.entity.SysOperLog;
import cn.swiftadmin.system.api.service.OperLogService;
import cn.swiftadmin.system.mapper.SysOperLogMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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

    @Override
    @Async
    public void insertLog(SysOperLog operLog) {
        save(operLog);
    }

    @Override
    public Page<SysOperLog> page(OperLogQueryDTO query) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_OPER_LOG.TITLE.like(PageHelper.likeIfPresent(query.getTitle())))
                .and(SYS_OPER_LOG.MODULE.like(PageHelper.likeIfPresent(query.getModule())))
                .and(SYS_OPER_LOG.OPER_NAME.like(PageHelper.likeIfPresent(query.getOperName())))
                .and(SYS_OPER_LOG.BUSINESS_TYPE.eq(query.getBusinessType(), query.getBusinessType() != null))
                .and(SYS_OPER_LOG.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .and(SYS_OPER_LOG.OPER_TIME.ge(query.getBeginTime(), query.getBeginTime() != null))
                .and(SYS_OPER_LOG.OPER_TIME.le(query.getEndTime(), query.getEndTime() != null))
                .orderBy(SYS_OPER_LOG.OPER_TIME.desc());

        return mapper.paginate(PageHelper.of(query.getPage(), query.getPageSize()), queryWrapper);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            mapper.deleteBatchByIds(ids);
        }
    }

    @Override
    public void clearAll() {
        mapper.deleteByQuery(QueryWrapper.create());
    }

}