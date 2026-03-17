package cn.swiftadmin.common.mybatis.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mybatisflex.core.keygen.IKeyGenerator;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 可配置的雪花算法 ID 生成器
 * <p>
 * 支持手动配置 workerId 和 datacenterId，适用于 Docker/K8s 等容器环境。
 * 未配置时自动根据时间戳和随机数生成，保证单机环境下的唯一性。
 * </p>
 *
 * <h3>ID 结构 (64位)：</h3>
 * <pre>
 * | 1位符号 | 41位时间戳 | 10位机器ID | 12位序列号 |
 * </pre>
 *
 * @author zhangpengju
 * @since 2026-03-15
 */
public class ConfigurableSnowflakeIdGenerator implements IKeyGenerator {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableSnowflakeIdGenerator.class);

    // ==================== 常量定义 ====================

    /** 起始时间戳 (2026-01-01 00:00:00 UTC) */
    public static final long TWEPOCH = 1735689600000L;

    /** 机器ID所占位数 */
    private static final long WORKER_ID_BITS = 5L;

    /** 数据中心ID所占位数 */
    private static final long DATACENTER_ID_BITS = 5L;

    /** 序列号所占位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大机器ID：31 */
    public static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 最大数据中心ID：31 */
    public static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /** 最大序列号：4095 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 机器ID左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 数据中心ID左移位数 */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间戳左移位数 */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    // ==================== 静态实例 ====================

    private static volatile ConfigurableSnowflakeIdGenerator defaultInstance;

    /**
     * 获取默认实例
     */
    public static ConfigurableSnowflakeIdGenerator getDefaultInstance() {
        if (defaultInstance == null) {
            synchronized (ConfigurableSnowflakeIdGenerator.class) {
                if (defaultInstance == null) {
                    defaultInstance = new ConfigurableSnowflakeIdGenerator();
                }
            }
        }
        return defaultInstance;
    }

    /**
     * 设置默认实例
     */
    public static void setDefaultInstance(ConfigurableSnowflakeIdGenerator instance) {
        defaultInstance = instance;
    }

    /**
     * 静态方法：生成下一个唯一ID
     */
    public static long nextIdStatic() {
        return getDefaultInstance().nextId();
    }

    // ==================== ID 解析 ====================

    /**
     * 从ID中解析出生成时间
     */
    public static long parseTimestamp(long id) {
        return (id >> TIMESTAMP_LEFT_SHIFT) + TWEPOCH;
    }

    /**
     * 从ID中解析出数据中心ID
     */
    public static long parseDatacenterId(long id) {
        return (id >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
    }

    /**
     * 从ID中解析出工作机器ID
     */
    public static long parseWorkerId(long id) {
        return (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    /**
     * 从ID中解析出序列号
     */
    public static long parseSequence(long id) {
        return id & SEQUENCE_MASK;
    }

    // ==================== 实例变量 ====================

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    // ==================== 构造方法 ====================

    /**
     * 构造函数（自动生成 workerId 和 datacenterId）
     */
    public ConfigurableSnowflakeIdGenerator() {
        this(-1, -1);
    }

    /**
     * 构造函数
     *
     * @param workerId     工作机器ID (0-31)，传 -1 自动生成
     * @param datacenterId 数据中心ID (0-31)，传 -1 自动生成
     */
    public ConfigurableSnowflakeIdGenerator(long workerId, long datacenterId) {
        // 自动生成 workerId 和 datacenterId
        if (workerId < 0) {
            workerId = generateAutoId(MAX_WORKER_ID);
        }
        if (datacenterId < 0) {
            datacenterId = generateAutoId(MAX_DATACENTER_ID);
        }

        // 参数校验
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("workerId 必须在 0 到 %d 之间", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenterId 必须在 0 到 %d 之间", MAX_DATACENTER_ID));
        }

        this.workerId = workerId;
        this.datacenterId = datacenterId;

        log.debug("雪花ID生成器初始化: workerId={}, datacenterId={}", workerId, datacenterId);
    }

    /**
     * 自动生成 ID（基于时间戳和随机数）
     */
    private long generateAutoId(long maxId) {
        // 使用当前时间戳的低 5 位 + 随机数，保证不同实例生成的 ID 不同
        long timestamp = System.currentTimeMillis();
        long id = (timestamp ^ ThreadLocalRandom.current().nextLong()) & maxId;
        return Math.abs(id) % (maxId + 1);
    }

    // ==================== IKeyGenerator 接口实现 ====================

    @Override
    public Object generate(Object entity, String keyColumn) {
        return nextId();
    }

    // ==================== ID 生成 ====================

    /**
     * 生成下一个唯一ID（线程安全）
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 小范围回拨，等待时钟追上
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(
                                String.format("时钟回拨，拒绝生成ID，回拨毫秒数: %d", lastTimestamp - timestamp));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待时钟追上时被中断", e);
                }
            } else {
                throw new RuntimeException(
                        String.format("时钟回拨，拒绝生成ID，回拨毫秒数: %d", lastTimestamp - timestamp));
            }
        }

        // 同一毫秒内
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列号溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装ID
        return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 阻塞到下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    // ==================== Getter ====================

    public long getWorkerId() {
        return workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }
}