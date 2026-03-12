package cn.swiftadmin.system.service.impl;

import cn.swiftadmin.system.api.entity.SysPasswordHistory;
import cn.swiftadmin.system.api.service.PasswordHistoryService;
import cn.swiftadmin.system.mapper.SysPasswordHistoryMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static cn.swiftadmin.system.api.entity.table.SysPasswordHistoryTableDef.SYS_PASSWORD_HISTORY;

/**
 * 密码历史服务实现类
 * <p>使用 MyBatis-Flex QueryWrapper 进行类型安全的数据库操作</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Service
@RequiredArgsConstructor
public class PasswordHistoryServiceImpl extends ServiceImpl<SysPasswordHistoryMapper, SysPasswordHistory> implements PasswordHistoryService {

    private final PasswordEncoder passwordEncoder;

    /**
     * 获取用户最近的密码哈希列表
     */
    @Override
    public List<String> getRecentPasswordHashes(Long userId, int limit) {
        return listAs(QueryWrapper.create()
                .select(SYS_PASSWORD_HISTORY.PASSWORD_HASH)
                .where(SYS_PASSWORD_HISTORY.USER_ID.eq(userId))
                .orderBy(SYS_PASSWORD_HISTORY.CREATE_TIME, false)
                .limit(limit), String.class);
    }

    /**
     * 添加密码历史记录
     */
    @Override
    public void addHistory(Long userId, String passwordHash) {
        SysPasswordHistory history = SysPasswordHistory.builder()
                .userId(userId)
                .passwordHash(passwordHash)
                .createTime(LocalDateTime.now())
                .build();
        save(history);
    }

    /**
     * 清理旧密码历史（保留最近的 N 条记录）
     * <p>使用子查询找出需要保留的记录 ID，删除其余记录</p>
     */
    @Override
    public void cleanOldHistory(Long userId, int keepCount) {
        // 先查询需要保留的最小 ID
        QueryWrapper keepWrapper = QueryWrapper.create()
                .select(SYS_PASSWORD_HISTORY.ID)
                .where(SYS_PASSWORD_HISTORY.USER_ID.eq(userId))
                .orderBy(SYS_PASSWORD_HISTORY.CREATE_TIME, false)
                .limit(keepCount);

        List<Long> keepIds = listAs(keepWrapper, Long.class);

        if (keepIds.isEmpty()) {
            return;
        }

        // 删除不在保留列表中的记录
        remove(QueryWrapper.create()
                .where(SYS_PASSWORD_HISTORY.USER_ID.eq(userId))
                .and(SYS_PASSWORD_HISTORY.ID.notIn(keepIds)));
    }

    /**
     * 检查密码是否在历史记录中
     */
    @Override
    public boolean isPasswordInHistory(Long userId, String rawPassword, int checkCount) {
        List<String> recentHashes = getRecentPasswordHashes(userId, checkCount);
        for (String hash : recentHashes) {
            if (passwordEncoder.matches(rawPassword, hash)) {
                return true;
            }
        }
        return false;
    }

}