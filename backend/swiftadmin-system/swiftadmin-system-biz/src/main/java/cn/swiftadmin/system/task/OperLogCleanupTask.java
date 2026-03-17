package cn.swiftadmin.system.task;

import cn.swiftadmin.system.api.service.OperLogService;
import cn.swiftadmin.system.api.util.ConfigUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志清理任务
 * <p>根据系统配置的保留天数定期清理过期的操作日志</p>
 *
 * @author zhangpengju
 * @since 2026-03-17
 */
@Slf4j
@Component("operLogCleanupTask")
@RequiredArgsConstructor
public class OperLogCleanupTask {

    private final OperLogService operLogService;

    /** 默认保留天数 */
    private static final int DEFAULT_KEEP_DAYS = 90;

    /** 最小保留天数（防止配置错误导致误删） */
    private static final int MIN_KEEP_DAYS = 7;

    /** 分批删除，每批数量 */
    private static final int BATCH_SIZE = 1000;

    /**
     * 执行操作日志清理
     * <p>由定时任务调度器调用</p>
     *
     * @return 清理的记录数
     */
    public int execute() {
        log.info("开始执行操作日志清理任务");
        long startTime = System.currentTimeMillis();

        try {
            // 从系统配置读取保留天数
            int keepDays = ConfigUtils.getIntValue("log.oper.keep_days", DEFAULT_KEEP_DAYS);

            // 配置校验：防止配置错误导致误删数据
            if (keepDays < MIN_KEEP_DAYS) {
                log.warn("操作日志保留天数配置值({})小于最小值({}), 使用最小值", keepDays, MIN_KEEP_DAYS);
                keepDays = MIN_KEEP_DAYS;
            }

            // 计算截止时间
            LocalDateTime beforeTime = LocalDateTime.now().minusDays(keepDays);
            log.info("操作日志保留天数: {}, 截止时间: {}", keepDays, beforeTime);

            // 分批删除过期日志
            int deletedCount = operLogService.deleteByOperTimeBefore(beforeTime, BATCH_SIZE);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("操作日志清理完成, 共删除 {} 条记录, 耗时 {} ms", deletedCount, costTime);

            return deletedCount;
        } catch (Exception e) {
            log.error("操作日志清理任务执行失败", e);
            throw e;
        }
    }

}