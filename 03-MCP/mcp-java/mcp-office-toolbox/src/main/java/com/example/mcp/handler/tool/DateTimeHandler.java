package com.example.mcp.handler.tool;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.LogUtil;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 日期时间工具处理器，提供日期加减、日期差计算、格式转换、时区转换、工作日计算、日历生成和星期查询等 MCP 工具。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class DateTimeHandler extends BaseHandler {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_DATETIME_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter CHINESE_DATE = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // ==================== 1. date_calc ====================

    /**
     * 日期加减：输入日期字符串和天数/月数/年数，返回新日期。
     *
     * @param dateStr 日期字符串，格式 yyyy-MM-dd
     * @param days    加减的天数（正数为加，负数为减），默认 0
     * @param months  加减的月数，默认 0
     * @param years   加减的年数，默认 0
     * @return 计算后的日期字符串
     */
    @Tool(name = "date_calc", description = "日期加减计算：输入日期和天数/月数/年数，返回计算后的新日期。")
    public String dateCalc(@ToolParam(description = "日期字符串，格式 yyyy-MM-dd") String dateStr,
        @ToolParam(description = "加减的天数（正数为加，负数为减），默认 0", required = false) Integer days,
        @ToolParam(description = "加减的月数（正数为加，负数为减），默认 0", required = false) Integer months,
        @ToolParam(description = "加减的年数（正数为加，负数为减），默认 0", required = false) Integer years) {
        if (dateStr == null || dateStr.isBlank()) {
            return "错误: 日期字符串不能为空";
        }
        int d = days != null ? days : 0;
        int m = months != null ? months : 0;
        int y = years != null ? years : 0;
        LogUtil.info("开始日期计算: {} + {}天 + {}月 + {}年", dateStr, d, m, y);

        return execute("dateCalc", () -> {
            LocalDate date = LocalDate.parse(dateStr, ISO_DATE);
            LocalDate result = date.plusYears(y)
                                   .plusMonths(m)
                                   .plusDays(d);
            LogUtil.info("日期计算完成: {}", result);
            return String.format("原始日期: %s\n计算后日期: %s\n变化: %d年 %d月 %d天", dateStr, result.format(ISO_DATE), y, m, d);
        });
    }

    // ==================== 2. date_diff ====================

    /**
     * 计算两个日期之间相差的天数/月数/年数。
     *
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd
     * @param unit      差值的单位：days（天）、months（月）、years（年），默认 days
     * @return 日期差值
     */
    @Tool(name = "date_diff", description = "计算两个日期之间相差的天数/月数/年数。")
    public String dateDiff(@ToolParam(description = "开始日期，格式 yyyy-MM-dd") String startDate,
        @ToolParam(description = "结束日期，格式 yyyy-MM-dd") String endDate,
        @ToolParam(description = "差值单位：days（天）、months（月）、years（年），默认 days", required = false) String unit) {
        if (startDate == null || startDate.isBlank()) {
            return "错误: 开始日期不能为空";
        }
        if (endDate == null || endDate.isBlank()) {
            return "错误: 结束日期不能为空";
        }
        String u = (unit != null && !unit.isBlank()) ? unit.trim()
                                                           .toLowerCase() : "days";
        LogUtil.info("开始计算日期差值: {} ~ {}，单位: {}", startDate, endDate, u);

        return execute("dateDiff", () -> {
            LocalDate start = LocalDate.parse(startDate, ISO_DATE);
            LocalDate end = LocalDate.parse(endDate, ISO_DATE);

            long diff;
            String unitName;
            switch (u) {
                case "days" -> {
                    diff = ChronoUnit.DAYS.between(start, end);
                    unitName = "天";
                }
                case "months" -> {
                    diff = ChronoUnit.MONTHS.between(start, end);
                    unitName = "月";
                }
                case "years" -> {
                    Period period = Period.between(start, end);
                    diff = period.getYears();
                    unitName = "年";
                    long totalMonths = ChronoUnit.MONTHS.between(start, end);
                    long totalDays = ChronoUnit.DAYS.between(start, end);
                    LogUtil.info("日期差值计算完成: {} 年", diff);
                    return String.format("从 %s 到 %s 相差:\n  %d 年 %d 月 %d 天\n  总计约 %d 个月\n  总计 %d 天", startDate, endDate,
                                         period.getYears(), Math.abs(period.getMonths()), Math.abs(period.getDays()), totalMonths, totalDays);
                }
                default -> {
                    return "错误: 不支持的单位 '" + unit + "'，可选值：days、months、years";
                }
            }

            LogUtil.info("日期差值计算完成: {} {}", diff, unitName);
            return String.format("从 %s 到 %s 相差: %d %s", startDate, endDate, diff, unitName);
        });
    }

    // ==================== 3. date_format ====================

    /**
     * 日期格式转换：时间戳与日期字符串互转，支持多种格式。
     *
     * @param input      输入的日期字符串或时间戳
     * @param fromFormat 输入格式
     * @param toFormat   输出格式
     * @return 转换后的日期字符串或时间戳
     */
    @Tool(name = "date_format", description = "日期格式转换：支持时间戳与日期字符串互转，支持多种常用日期格式。")
    public String dateFormat(@ToolParam(description = "输入的日期字符串或时间戳（字符串格式）") String input,
        @ToolParam(description = "输入格式：yyyy-MM-dd、yyyy-MM-dd HH:mm:ss、yyyy-MM-dd HH:mm:ss.SSS、yyyy年MM月dd日、yyyy/MM/dd、timestamp_s、timestamp_ms") String fromFormat,
        @ToolParam(description = "输出格式：yyyy-MM-dd、yyyy-MM-dd HH:mm:ss、yyyy-MM-dd HH:mm:ss.SSS、yyyy年MM月dd日、yyyy/MM/dd、timestamp_s、timestamp_ms") String toFormat) {
        if (input == null || input.isBlank()) {
            return "错误: 输入内容不能为空";
        }
        if (fromFormat == null || fromFormat.isBlank()) {
            return "错误: 输入格式不能为空";
        }
        if (toFormat == null || toFormat.isBlank()) {
            return "错误: 输出格式不能为空";
        }
        LogUtil.info("开始日期格式转换: {} -> {}", fromFormat, toFormat);

        return execute("dateFormat", () -> {
            LocalDateTime dateTime;
            if ("timestamp_s".equalsIgnoreCase(fromFormat)) {
                long seconds = Long.parseLong(input.trim());
                dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
            } else if ("timestamp_ms".equalsIgnoreCase(fromFormat)) {
                long millis = Long.parseLong(input.trim());
                dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            } else {
                DateTimeFormatter inFmt = getFormatter(fromFormat);
                if (inFmt == null) {
                    return "错误: 不支持的输入格式 '" + fromFormat + "'";
                }
                try {
                    dateTime = LocalDateTime.parse(input.trim(), inFmt);
                } catch (DateTimeParseException e1) {
                    try {
                        LocalDate date = LocalDate.parse(input.trim(), inFmt);
                        dateTime = date.atStartOfDay();
                    } catch (DateTimeParseException e2) {
                        return "错误: 输入内容与指定格式 '" + fromFormat + "' 不匹配";
                    }
                }
            }

            String result;
            if ("timestamp_s".equalsIgnoreCase(toFormat)) {
                long seconds = dateTime.atZone(ZoneId.systemDefault())
                                       .toEpochSecond();
                result = String.valueOf(seconds);
            } else if ("timestamp_ms".equalsIgnoreCase(toFormat)) {
                long millis = dateTime.atZone(ZoneId.systemDefault())
                                      .toInstant()
                                      .toEpochMilli();
                result = String.valueOf(millis);
            } else {
                DateTimeFormatter outFmt = getFormatter(toFormat);
                if (outFmt == null) {
                    return "错误: 不支持的输出格式 '" + toFormat + "'";
                }
                result = dateTime.format(outFmt);
            }

            LogUtil.info("日期格式转换完成");
            return result;
        });
    }

    private DateTimeFormatter getFormatter(String format) {
        return switch (format.trim()) {
            case "yyyy-MM-dd" -> ISO_DATE;
            case "yyyy-MM-dd HH:mm:ss" -> ISO_DATETIME;
            case "yyyy-MM-dd HH:mm:ss.SSS" -> ISO_DATETIME_MS;
            case "yyyy年MM月dd日" -> CHINESE_DATE;
            case "yyyy/MM/dd" -> SLASH_DATE;
            default -> null;
        };
    }

    // ==================== 4. timezone_convert ====================

    /**
     * 时区转换：将日期时间从一个时区转换到另一个时区。
     *
     * @param dateTimeStr 日期时间字符串，格式 yyyy-MM-dd HH:mm:ss
     * @param fromZone    源时区，如 Asia/Shanghai
     * @param toZone      目标时区，如 America/New_York
     * @return 转换后的日期时间字符串
     */
    @Tool(name = "timezone_convert", description = "时区转换：将日期时间从一个时区转换到另一个时区。时区格式如 Asia/Shanghai、America/New_York。")
    public String timezoneConvert(@ToolParam(description = "日期时间字符串，格式 yyyy-MM-dd HH:mm:ss") String dateTimeStr,
        @ToolParam(description = "源时区，如 Asia/Shanghai") String fromZone, @ToolParam(description = "目标时区，如 America/New_York") String toZone) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return "错误: 日期时间字符串不能为空";
        }
        if (fromZone == null || fromZone.isBlank()) {
            return "错误: 源时区不能为空";
        }
        if (toZone == null || toZone.isBlank()) {
            return "错误: 目标时区不能为空";
        }
        LogUtil.info("开始时区转换: {} [{}] -> [{}]", dateTimeStr, fromZone, toZone);

        return execute("timezoneConvert", () -> {
            ZoneId fromZid = ZoneId.of(fromZone.trim());
            ZoneId toZid = ZoneId.of(toZone.trim());
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr.trim(), ISO_DATETIME);
            ZonedDateTime fromZdt = localDateTime.atZone(fromZid);
            ZonedDateTime toZdt = fromZdt.withZoneSameInstant(toZid);
            String result = toZdt.format(ISO_DATETIME);
            LogUtil.info("时区转换完成: {}", result);
            return String.format("原始时间: %s [%s]\n转换后时间: %s [%s]\n时区偏移: %s", dateTimeStr, fromZone, result, toZone, toZdt.getOffset()
                                                                                                                                     .toString());
        });
    }

    // ==================== 5. workday_calc ====================

    /**
     * 工作日计算：计算两个日期之间的工作日天数（排除周六日）。
     *
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd
     * @return 工作日天数及详细统计
     */
    @Tool(name = "workday_calc", description = "工作日计算：计算两个日期之间的工作日天数（排除周六日）。")
    public String workdayCalc(@ToolParam(description = "开始日期，格式 yyyy-MM-dd") String startDate,
        @ToolParam(description = "结束日期，格式 yyyy-MM-dd") String endDate) {
        if (startDate == null || startDate.isBlank()) {
            return "错误: 开始日期不能为空";
        }
        if (endDate == null || endDate.isBlank()) {
            return "错误: 结束日期不能为空";
        }
        LogUtil.info("开始计算工作日: {} ~ {}", startDate, endDate);

        return execute("workdayCalc", () -> {
            LocalDate start = LocalDate.parse(startDate, ISO_DATE);
            LocalDate end = LocalDate.parse(endDate, ISO_DATE);
            if (start.isAfter(end)) {
                LocalDate temp = start;
                start = end;
                end = temp;
            }

            int workDays = 0;
            int weekendDays = 0;
            int totalDays = 0;
            LocalDate current = start;
            while (!current.isAfter(end)) {
                totalDays++;
                DayOfWeek dow = current.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    weekendDays++;
                } else {
                    workDays++;
                }
                current = current.plusDays(1);
            }

            LogUtil.info("工作日计算完成: {} 个工作日", workDays);
            return String.format("日期范围: %s 至 %s\n总天数: %d 天\n工作日: %d 天\n周末日: %d 天\n", start.format(ISO_DATE), end.format(ISO_DATE),
                                 totalDays, workDays, weekendDays);
        });
    }

    // ==================== 6. calendar_generate ====================

    /**
     * 生成指定年月的文本日历。
     *
     * @param year  年份，如 2026
     * @param month 月份（1-12），如 7
     * @return 格式化的日历文本
     */
    @Tool(name = "calendar_generate", description = "生成指定年月的文本日历，返回格式化后的日历视图。")
    public String calendarGenerate(@ToolParam(description = "年份，如 2026") int year, @ToolParam(description = "月份（1-12），如 7") int month) {
        if (month < 1 || month > 12) {
            return "错误: 月份必须在 1-12 之间";
        }
        LogUtil.info("开始生成日历: {}年{}月", year, month);

        return execute("calendarGenerate", () -> {
            LocalDate firstDay = LocalDate.of(year, month, 1);
            int daysInMonth = firstDay.lengthOfMonth();
            int startDayOfWeek = firstDay.getDayOfWeek()
                                         .getValue();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("       %d年 %d月\n", year, month));
            sb.append(" 一  二  三  四  五  六  日\n");

            for (int i = 1; i < startDayOfWeek; i++) {
                sb.append("    ");
            }

            for (int day = 1; day <= daysInMonth; day++) {
                sb.append(String.format("%3d ", day));
                int currentDow = (startDayOfWeek + day - 1) % 7;
                if (currentDow == 0) {
                    sb.append("\n");
                }
            }

            String result = sb.toString();
            if (!result.endsWith("\n")) {
                result += "\n";
            }
            LogUtil.info("日历生成完成");
            return result;
        });
    }

    // ==================== 7. date_weekday ====================

    /**
     * 查询指定日期是星期几，返回中英文名称。
     *
     * @param dateStr 日期字符串，格式 yyyy-MM-dd
     * @return 星期几的中英文名称
     */
    @Tool(name = "date_weekday", description = "查询指定日期是星期几，返回中文和英文名称。")
    public String dateWeekday(@ToolParam(description = "日期字符串，格式 yyyy-MM-dd") String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return "错误: 日期字符串不能为空";
        }
        LogUtil.info("开始查询星期: {}", dateStr);

        return execute("dateWeekday", () -> {
            LocalDate date = LocalDate.parse(dateStr, ISO_DATE);
            DayOfWeek dow = date.getDayOfWeek();

            String chineseName = switch (dow) {
                case MONDAY -> "星期一";
                case TUESDAY -> "星期二";
                case WEDNESDAY -> "星期三";
                case THURSDAY -> "星期四";
                case FRIDAY -> "星期五";
                case SATURDAY -> "星期六";
                case SUNDAY -> "星期日";
            };

            String englishName = dow.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            LogUtil.info("星期查询完成: {}", chineseName);

            return String.format("日期: %s\n中文: %s\n英文: %s\n是否为周末: %s", dateStr, chineseName, englishName,
                                 (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) ? "是" : "否");
        });
    }
}
