package cn.swiftadmin.system.mapper;

import cn.swiftadmin.system.api.entity.SysAuditLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

}