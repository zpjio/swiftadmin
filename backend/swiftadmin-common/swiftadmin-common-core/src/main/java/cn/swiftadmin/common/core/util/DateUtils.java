package cn.swiftadmin.common.core.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 日期时间工具类
 * <p>提供日期时间的格式化、解析、计算等常用方法</p>
 * <p>使用 Java 8 Time API，线程安全且性能优异</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
public final class DateUtils {

    private DateUtils() {
    }

    /** 默认日期时间格式：yyyy-MM-dd HH:mm:ss */
    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 默认日期格式：yyyy-MM-dd */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /** 默认时间格式：HH:mm:ss */
    public static final String TIME_PATTERN = "HH:mm:ss";

    /** 紧凑格式：yyyyMMddHHmmss */
    public static final String COMPACT_PATTERN = "yyyyMMddHHmmss";

    /** 中文格式：yyyy 年 MM 月 dd 日 HH 时 mm 分 ss 秒 */
    public static final String CHINESE_PATTERN = "yyyy 年 MM 月 dd 日 HH 时 mm 分 ss 秒";

    /** 日期时间格式化器（线程安全） */
    public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);

    /** 日期格式化器 */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /** 时间格式化器 */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    /** 紧凑格式格式化器 */
    public static final DateTimeFormatter COMPACT_FORMATTER = DateTimeFormatter.ofPattern(COMPACT_PATTERN);

    /** 中文格式格式化器 */
    public static final DateTimeFormatter CHINESE_FORMATTER = DateTimeFormatter.ofPattern(CHINESE_PATTERN);

    /** ISO 标准日期时间格式化器 */
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 格式化器缓存（线程安全） */
    private static final ConcurrentMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    /** 系统默认时区 */
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    /**
     * 获取格式化器（带缓存）
     *
     * @param pattern 格式模式
     * @return 格式化器
     */
    private static DateTimeFormatter getFormatter(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }

    /**
     * 格式化日期时间（使用默认格式）
     *
     * @param dateTime 日期时间对象
     * @return 格式化后的字符串，null 返回 null
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DEFAULT_FORMATTER) : null;
    }

    /**
     * 格式化日期时间（使用指定格式）
     *
     * @param dateTime 日期时间对象
     * @param pattern  格式模式
     * @return 格式化后的字符串，null 返回 null
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime != null ? dateTime.format(getFormatter(pattern)) : null;
    }

    /**
     * 格式化日期（仅日期部分）
     *
     * @param dateTime 日期时间对象
     * @return 格式化后的日期字符串，null 返回 null
     */
    public static String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : null;
    }

    /**
     * 格式化时间（仅时间部分）
     *
     * @param dateTime 日期时间对象
     * @return 格式化后的时间字符串，null 返回 null
     */
    public static String formatTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIME_FORMATTER) : null;
    }

    /**
     * 解析日期时间字符串（使用默认格式）
     *
     * @param dateStr 日期时间字符串
     * @return 日期时间对象，null 返回 null
     */
    public static LocalDateTime parse(String dateStr) {
        return dateStr != null ? LocalDateTime.parse(dateStr, DEFAULT_FORMATTER) : null;
    }

    /**
     * 解析日期时间字符串（使用指定格式）
     *
     * @param dateStr 日期时间字符串
     * @param pattern 格式模式
     * @return 日期时间对象，null 返回 null
     */
    public static LocalDateTime parse(String dateStr, String pattern) {
        return dateStr != null ? LocalDateTime.parse(dateStr, getFormatter(pattern)) : null;
    }

    /**
     * 解析 ISO 标准格式日期时间
     *
     * @param dateStr ISO 格式日期时间字符串（如 2026-03-11T10:30:00）
     * @return 日期时间对象，null 返回 null
     */
    public static LocalDateTime parseIso(String dateStr) {
        return dateStr != null ? LocalDateTime.parse(dateStr, ISO_FORMATTER) : null;
    }

    /**
     * 解析日期字符串（仅日期部分）
     *
     * @param dateStr 日期字符串
     * @return 日期对象，null 返回 null
     */
    public static LocalDate parseDate(String dateStr) {
        return dateStr != null ? LocalDate.parse(dateStr, DATE_FORMATTER) : null;
    }

    /**
     * 获取当前日期时间
     *
     * @return 当前日期时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前日期
     *
     * @return 当前日期
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public static LocalTime nowTime() {
        return LocalTime.now();
    }

    /**
     * 日期时间转时间戳（秒）
     *
     * @param dateTime 日期时间
     * @return 时间戳（秒）
     */
    public static long toEpochSecond(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toEpochSecond(ZoneOffset.ofHours(8)) : 0L;
    }

    /**
     * 日期时间转时间戳（毫秒）
     *
     * @param dateTime 日期时间
     * @return 时间戳（毫秒）
     */
    public static long toEpochMilli(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli() : 0L;
    }

    /**
     * 时间戳转日期时间（秒）
     *
     * @param epochSecond 时间戳（秒）
     * @return 日期时间
     */
    public static LocalDateTime fromEpochSecond(long epochSecond) {
        return LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.ofHours(8));
    }

    /**
     * 时间戳转日期时间（毫秒）
     *
     * @param epochMilli 时间戳（毫秒）
     * @return 日期时间
     */
    public static LocalDateTime fromEpochMilli(long epochMilli) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMilli), DEFAULT_ZONE_ID);
    }

    /**
     * 日期转 LocalDateTime
     *
     * @param date Date 对象
     * @return LocalDateTime，null 返回 null
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * LocalDateTime 转 Date
     *
     * @param localDateTime LocalDateTime 对象
     * @return Date，null 返回 null
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(DEFAULT_ZONE_ID).toInstant());
    }

    /**
     * 增加天数
     *
     * @param dateTime 日期时间
     * @param days 天数
     * @return 增加后的日期时间
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        return dateTime != null ? dateTime.plusDays(days) : null;
    }

    /**
     * 减少天数
     *
     * @param dateTime 日期时间
     * @param days 天数
     * @return 减少后的日期时间
     */
    public static LocalDateTime minusDays(LocalDateTime dateTime, long days) {
        return dateTime != null ? dateTime.minusDays(days) : null;
    }

    /**
     * 增加月数
     *
     * @param dateTime 日期时间
     * @param months 月数
     * @return 增加后的日期时间
     */
    public static LocalDateTime plusMonths(LocalDateTime dateTime, long months) {
        return dateTime != null ? dateTime.plusMonths(months) : null;
    }

    /**
     * 减少月数
     *
     * @param dateTime 日期时间
     * @param months 月数
     * @return 减少后的日期时间
     */
    public static LocalDateTime minusMonths(LocalDateTime dateTime, long months) {
        return dateTime != null ? dateTime.minusMonths(months) : null;
    }

    /**
     * 增加年数
     *
     * @param dateTime 日期时间
     * @param years 年数
     * @return 增加后的日期时间
     */
    public static LocalDateTime plusYears(LocalDateTime dateTime, long years) {
        return dateTime != null ? dateTime.plusYears(years) : null;
    }

    /**
     * 减少年数
     *
     * @param dateTime 日期时间
     * @param years 年数
     * @return 减少后的日期时间
     */
    public static LocalDateTime minusYears(LocalDateTime dateTime, long years) {
        return dateTime != null ? dateTime.minusYears(years) : null;
    }

    /**
     * 获取月初
     *
     * @param dateTime 日期时间
     * @return 月初日期时间
     */
    public static LocalDateTime getMonthStart(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.with(TemporalAdjusters.firstDayOfMonth()) : null;
    }

    /**
     * 获取月末
     *
     * @param dateTime 日期时间
     * @return 月末日期时间
     */
    public static LocalDateTime getMonthEnd(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.with(TemporalAdjusters.lastDayOfMonth()) : null;
    }

    /**
     * 获取年初
     *
     * @param dateTime 日期时间
     * @return 年初日期时间
     */
    public static LocalDateTime getYearStart(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.with(TemporalAdjusters.firstDayOfYear()) : null;
    }

    /**
     * 获取年末
     *
     * @param dateTime 日期时间
     * @return 年末日期时间
     */
    public static LocalDateTime getYearEnd(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.with(TemporalAdjusters.lastDayOfYear()) : null;
    }

    /**
     * 获取周一
     *
     * @param dateTime 日期时间
     * @return 周一日期时间
     */
    public static LocalDateTime getWeekStart(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : null;
    }

    /**
     * 获取周日
     *
     * @param dateTime 日期时间
     * @return 周日日期时间
     */
    public static LocalDateTime getWeekEnd(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)) : null;
    }

    /**
     * 设置时间到当天开始（00:00:00）
     *
     * @param dateTime 日期时间
     * @return 当天开始的日期时间
     */
    public static LocalDateTime withDayStart(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.with(LocalTime.MIN) : null;
    }

    /**
     * 设置时间到当天结束（23:59:59）
     *
     * @param dateTime 日期时间
     * @return 当天结束的日期时间
     */
    public static LocalDateTime withDayEnd(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.with(LocalTime.MAX) : null;
    }

    /**
     * 判断是否是今天
     *
     * @param dateTime 日期时间
     * @return true=是今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        return dateTime != null && dateTime.toLocalDate().equals(LocalDate.now());
    }

    /**
     * 判断是否是昨天
     *
     * @param dateTime 日期时间
     * @return true=是昨天
     */
    public static boolean isYesterday(LocalDateTime dateTime) {
        return dateTime != null && dateTime.toLocalDate().equals(LocalDate.now().minusDays(1));
    }

    /**
     * 判断是否是本周
     *
     * @param dateTime 日期时间
     * @return true=是本周
     */
    public static boolean isThisWeek(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate date = dateTime.toLocalDate();
        return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
    }

    /**
     * 判断是否是本月
     *
     * @param dateTime 日期时间
     * @return true=是本月
     */
    public static boolean isThisMonth(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        LocalDate date = dateTime.toLocalDate();
        return now.getYear() == date.getYear() && now.getMonthValue() == date.getMonthValue();
    }

    /**
     * 计算两个日期之间的天数差
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 天数差
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return java.time.Duration.between(start, end).toDays();
    }

    /**
     * 计算两个日期之间的天数差（仅日期部分）
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 天数差
     */
    public static long daysBetweenDate(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期之间的月数差
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 月数差
     */
    public static long monthsBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.MONTHS.between(start, end);
    }

    /**
     * 判断日期是否在指定范围内
     *
     * @param dateTime 日期时间
     * @param start 开始日期
     * @param end 结束日期
     * @return true=在范围内
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        if (dateTime == null || start == null || end == null) {
            return false;
        }
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }
}
