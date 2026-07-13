package com.example.mcp.handler.i18n;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.LogUtil;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 国际化与本地化工具，提供单位换算、货币汇率换算和数字格式化功能。
 * 使用 JDK 21 内置能力实现，不依赖外部库。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class I18nHandler extends BaseHandler {

    // ==================== 长度换算系数（以米为基准） ====================
    private static final Map<String, Double> LENGTH_TO_METER = Map.ofEntries(Map.entry("mi", 1609.344),      // 英里
                                                                             Map.entry("km", 1000.0),        // 公里
                                                                             Map.entry("m", 1.0),            // 米
                                                                             Map.entry("cm", 0.01),          // 厘米
                                                                             Map.entry("mm", 0.001),         // 毫米
                                                                             Map.entry("ft", 0.3048),        // 英尺
                                                                             Map.entry("in", 0.0254),        // 英寸
                                                                             Map.entry("yd", 0.9144)         // 码
    );

    // ==================== 重量换算系数（以克为基准） ====================
    private static final Map<String, Double> WEIGHT_TO_GRAM = Map.ofEntries(Map.entry("kg", 1000.0),        // 公斤
                                                                            Map.entry("g", 1.0),            // 克
                                                                            Map.entry("lb", 453.59237),     // 磅
                                                                            Map.entry("oz", 28.349523125)   // 盎司
    );

    // ==================== 面积换算系数（以平方米为基准） ====================
    private static final Map<String, Double> AREA_TO_SQM = Map.ofEntries(Map.entry("sqm", 1.0),          // 平方米
                                                                         Map.entry("sqft", 0.09290304),  // 平方英尺
                                                                         Map.entry("mu", 666.6667),      // 亩
                                                                         Map.entry("ha", 10000.0)        // 公顷
    );

    // ==================== 默认汇率（以 CNY 为基准） ====================
    private static final Map<String, Double> DEFAULT_RATES = new HashMap<>();
    // ==================== 中文数字字符 ====================
    private static final char[] CN_DIGITS = {'零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'};
    private static final char[] CN_DIGITS_SIMPLE = {'零', '一', '二', '三', '四', '五', '六', '七', '八', '九'};
    private static final String[] CN_UNITS = {"", "拾", "佰", "仟"};
    private static final String[] CN_UNITS_BIG = {"", "万", "亿", "万亿"};
    // ==================== 罗马数字 ====================
    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    static {
        DEFAULT_RATES.put("CNY", 1.0);
        DEFAULT_RATES.put("USD", 7.25);
        DEFAULT_RATES.put("EUR", 7.93);
        DEFAULT_RATES.put("JPY", 0.048);
        DEFAULT_RATES.put("GBP", 9.22);
        DEFAULT_RATES.put("HKD", 0.93);
        DEFAULT_RATES.put("KRW", 0.0054);
        DEFAULT_RATES.put("AUD", 4.77);
        DEFAULT_RATES.put("CAD", 5.32);
        DEFAULT_RATES.put("SGD", 5.42);
        DEFAULT_RATES.put("CHF", 8.12);
        DEFAULT_RATES.put("RUB", 0.08);
        DEFAULT_RATES.put("INR", 0.087);
        DEFAULT_RATES.put("BRL", 1.45);
    }

    /**
     * 获取标准化的单位标识符
     */
    private String normalizeUnit(String unit) {
        if (unit == null) {
            return "";
        }
        return unit.trim()
                   .toLowerCase();
    }

    // --- 1. i18n_unit_convert ---

    /**
     * 单位换算工具，支持长度、重量、温度和面积四种类型的单位换算。
     * <p>
     * 支持的单位：
     * - 长度: mi(英里), km(公里), m(米), cm(厘米), mm(毫米), ft(英尺), in(英寸), yd(码)
     * - 重量: kg(公斤), g(克), lb(磅), oz(盎司)
     * - 温度: C(摄氏度), F(华氏度), K(开尔文)
     * - 面积: sqm(平方米), sqft(平方英尺), mu(亩), ha(公顷)
     * </p>
     *
     * @param value    要换算的数值
     * @param fromUnit 源单位
     * @param toUnit   目标单位
     * @param category 换算类别: length(长度), weight(重量), temperature(温度), area(面积)
     * @return 换算结果
     */
    @Tool(name = "i18n_unit_convert", description = "单位换算工具。支持长度(mi/km/m/cm/mm/ft/in/yd)、重量(kg/g/lb/oz)、温度(℃/℉/K)、面积(㎡/sqft/亩/公顷)的换算。")
    public String i18nUnitConvert(@ToolParam(description = "要换算的数值") double value, @ToolParam(description = "源单位") String fromUnit,
        @ToolParam(description = "目标单位") String toUnit,
        @ToolParam(description = "换算类别: length(长度), weight(重量), temperature(温度), area(面积)") String category) {
        return execute("i18nUnitConvert", () -> {
            String from = normalizeUnit(fromUnit);
            String to = normalizeUnit(toUnit);
            String cat = (category != null && !category.isBlank()) ? category.toLowerCase()
                                                                             .trim() : "length";

            double result;
            String fromDisplay;
            String toDisplay;
            String formula;

            switch (cat) {
                case "length" -> {
                    Double fromFactor = LENGTH_TO_METER.get(from);
                    Double toFactor = LENGTH_TO_METER.get(to);
                    if (fromFactor == null) {
                        return "错误: 不支持的长度单位 '" + fromUnit + "'。支持: mi, km, m, cm, mm, ft, in, yd";
                    }
                    if (toFactor == null) {
                        return "错误: 不支持的长度单位 '" + toUnit + "'。支持: mi, km, m, cm, mm, ft, in, yd";
                    }
                    result = value * fromFactor / toFactor;
                    fromDisplay = from;
                    toDisplay = to;
                    formula = String.format("%s %s = %s %s", formatNumber(value), fromDisplay, formatNumber(result), toDisplay);
                }
                case "weight" -> {
                    Double fromFactor = WEIGHT_TO_GRAM.get(from);
                    Double toFactor = WEIGHT_TO_GRAM.get(to);
                    if (fromFactor == null) {
                        return "错误: 不支持的重量单位 '" + fromUnit + "'。支持: kg, g, lb, oz";
                    }
                    if (toFactor == null) {
                        return "错误: 不支持的重量单位 '" + toUnit + "'。支持: kg, g, lb, oz";
                    }
                    result = value * fromFactor / toFactor;
                    fromDisplay = from;
                    toDisplay = to;
                    formula = String.format("%s %s = %s %s", formatNumber(value), fromDisplay, formatNumber(result), toDisplay);
                }
                case "temperature" -> {
                    result = convertTemperature(value, from, to);
                    fromDisplay = getTemperatureDisplayName(from);
                    toDisplay = getTemperatureDisplayName(to);
                    formula = String.format("%s%s = %s%s", formatNumber(value), fromDisplay, formatNumber(result), toDisplay);
                }
                case "area" -> {
                    Double fromFactor = AREA_TO_SQM.get(from);
                    Double toFactor = AREA_TO_SQM.get(to);
                    if (fromFactor == null) {
                        return "错误: 不支持的面积单位 '" + fromUnit + "'。支持: sqm, sqft, mu, ha";
                    }
                    if (toFactor == null) {
                        return "错误: 不支持的面积单位 '" + toUnit + "'。支持: sqm, sqft, mu, ha";
                    }
                    result = value * fromFactor / toFactor;
                    fromDisplay = getAreaDisplayName(from);
                    toDisplay = getAreaDisplayName(to);
                    formula = String.format("%s %s = %s %s", formatNumber(value), fromDisplay, formatNumber(result), toDisplay);
                }
                default -> {
                    return "错误: 不支持的换算类别 '" + category + "'。支持: length, weight, temperature, area";
                }
            }

            LogUtil.info("i18nUnitConvert 完成: {} {} -> {} {} = {}", value, fromUnit, toUnit, category, result);
            return formula;
        });
    }

    /**
     * 温度换算
     */
    private double convertTemperature(double value, String from, String to) {
        if (from.equals(to)) {
            return value;
        }
        // 先转为摄氏度
        double celsius = switch (from) {
            case "c" -> value;
            case "f" -> (value - 32) * 5.0 / 9.0;
            case "k" -> value - 273.15;
            default -> throw new IllegalArgumentException("不支持的温度单位: " + from);
        };
        // 从摄氏度转到目标
        return switch (to) {
            case "c" -> celsius;
            case "f" -> celsius * 9.0 / 5.0 + 32;
            case "k" -> celsius + 273.15;
            default -> throw new IllegalArgumentException("不支持的温度单位: " + to);
        };
    }

    private String getTemperatureDisplayName(String unit) {
        return switch (unit) {
            case "c" -> "℃";
            case "f" -> "℉";
            case "k" -> "K";
            default -> unit;
        };
    }

    private String getAreaDisplayName(String unit) {
        return switch (unit) {
            case "sqm" -> "平方米";
            case "sqft" -> "平方英尺";
            case "mu" -> "亩";
            case "ha" -> "公顷";
            default -> unit;
        };
    }

    /**
     * 格式化数字，避免过长的小数
     */
    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        // 保留最多 6 位有效小数
        if (Math.abs(value) >= 1) {
            return String.format("%.4f", value)
                         .replaceAll("0+$", "")
                         .replaceAll("\\.$", "");
        } else {
            return String.format("%.6f", value)
                         .replaceAll("0+$", "")
                         .replaceAll("\\.$", "");
        }
    }

    // --- 2. i18n_currency_convert ---

    /**
     * 货币汇率换算工具，支持 CNY/USD/EUR/JPY/GBP/HKD/KRW 等常见货币之间的换算。
     * <p>
     * 支持两种汇率来源：
     * 1. 手动指定汇率（传入 rate 参数，表示 1 单位源货币 = ? 人民币）
     * 2. 使用内置默认参考汇率（不传 rate 参数时使用）
     * </p>
     * <p>
     * 注意：内置默认汇率为参考值，实际汇率请以实时市场汇率为准。
     * </p>
     *
     * @param amount       要换算的金额
     * @param fromCurrency 源货币代码
     * @param toCurrency   目标货币代码
     * @param rate         手动指定汇率: 1 单位 fromCurrency = ? CNY（可选，不传则使用默认汇率）
     * @return 换算结果
     */
    @Tool(name = "i18n_currency_convert", description = "货币汇率换算（支持 CNY/USD/EUR/JPY/GBP/HKD/KRW 等）。可指定汇率或使用默认参考汇率。")
    public String i18nCurrencyConvert(@ToolParam(description = "要换算的金额") double amount,
        @ToolParam(description = "源货币代码，如 CNY、USD、EUR") String fromCurrency,
        @ToolParam(description = "目标货币代码，如 CNY、USD、EUR") String toCurrency,
        @ToolParam(description = "手动指定汇率: 1 单位 fromCurrency = ? CNY（可选，不传则使用默认参考汇率）", required = false) Double rate) {
        return execute("i18nCurrencyConvert", () -> {
            String from = (fromCurrency != null) ? fromCurrency.trim()
                                                               .toUpperCase() : "CNY";
            String to = (toCurrency != null) ? toCurrency.trim()
                                                         .toUpperCase() : "USD";

            Double fromRate;
            Double toRate;

            if (rate != null && rate > 0) {
                // 使用手动指定的汇率（1 fromCurrency = rate CNY）
                fromRate = rate;
                // 目标货币汇率从默认汇率中获取，或使用 CNY
                toRate = DEFAULT_RATES.getOrDefault(to, 1.0);
            } else {
                // 使用默认汇率
                fromRate = DEFAULT_RATES.get(from);
                toRate = DEFAULT_RATES.get(to);

                if (fromRate == null) {
                    return "错误: 不支持的货币代码 '" + fromCurrency + "'，且未提供自定义汇率。" + "支持: " + String.join(", ", DEFAULT_RATES.keySet());
                }
                if (toRate == null) {
                    return "错误: 不支持的货币代码 '" + toCurrency + "'，且未提供自定义汇率。" + "支持: " + String.join(", ", DEFAULT_RATES.keySet());
                }
            }

            // 先转为 CNY，再转为目标货币
            double cnyAmount = amount * fromRate;
            double result = cnyAmount / toRate;

            String currencySymbol = getCurrencySymbol(to);
            String resultStr = String.format("%.2f", result);

            String note = (rate != null) ? "（使用自定义汇率: 1 " + from + " = " + rate + " CNY）" : "（使用默认参考汇率，实际汇率以市场为准）";

            LogUtil.info("i18nCurrencyConvert 完成: {} {} -> {} {} = {}", amount, from, to, result, currencySymbol);
            return String.format("%.2f %s = %s %s%s\n%s", amount, from, currencySymbol, resultStr, to, note);
        });
    }

    /**
     * 获取货币符号
     */
    private String getCurrencySymbol(String code) {
        return switch (code) {
            case "CNY" -> "¥";
            case "USD" -> "$";
            case "EUR" -> "€";
            case "JPY" -> "¥";
            case "GBP" -> "£";
            case "HKD" -> "HK$";
            case "KRW" -> "₩";
            case "AUD" -> "A$";
            case "CAD" -> "C$";
            case "SGD" -> "S$";
            case "CHF" -> "CHF ";
            default -> "";
        };
    }

    // --- 3. i18n_number_format ---

    /**
     * 数字格式化工具，支持千分位分隔、中文大写数字、中文小写数字和罗马数字格式。
     *
     * @param number     要格式化的数字
     * @param formatType 格式化类型: thousands(千分位分隔), chinese_upper(中文大写数字), chinese_lower(中文小写数字), roman(罗马数字)
     * @param locale     区域设置，仅对千分位格式有效，如 zh_CN、en_US（默认 zh_CN）
     * @return 格式化后的数字字符串
     */
    @Tool(name = "i18n_number_format", description = "数字格式化：支持千分位分隔、中文大写数字、中文小写数字、罗马数字。")
    public String i18nNumberFormat(@ToolParam(description = "要格式化的数字") long number,
        @ToolParam(description = "格式化类型: thousands(千分位分隔), chinese_upper(中文大写数字), chinese_lower(中文小写数字), roman(罗马数字)") String formatType,
        @ToolParam(description = "区域设置，仅对千分位格式有效，如 zh_CN、en_US（默认 zh_CN）", required = false) String locale) {
        return execute("i18nNumberFormat", () -> {
            String type = (formatType != null && !formatType.isBlank()) ? formatType.toLowerCase()
                                                                                    .trim() : "thousands";

            String result = switch (type) {
                case "thousands" -> formatThousands(number, locale);
                case "chinese_upper" -> formatChineseNumber(number, true);
                case "chinese_lower" -> formatChineseNumber(number, false);
                case "roman" -> formatRomanNumber(number);
                default -> "错误: 不支持的格式化类型 '" + formatType + "'。支持: thousands, chinese_upper, chinese_lower, roman";
            };

            LogUtil.info("i18nNumberFormat 完成: {} -> {} = {}", number, type, result);
            return result;
        });
    }

    /**
     * 千分位分隔格式化
     */
    private String formatThousands(long number, String localeStr) {
        Locale locale = parseLocale(localeStr);
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        return nf.format(number);
    }

    /**
     * 解析区域设置
     */
    private Locale parseLocale(String localeStr) {
        if (localeStr == null || localeStr.isBlank()) {
            return Locale.CHINA;
        }
        String[] parts = localeStr.split("_");
        if (parts.length == 2) {
            return Locale.of(parts[0], parts[1]);
        }
        return switch (localeStr.toLowerCase()) {
            case "zh" -> Locale.CHINESE;
            case "en" -> Locale.ENGLISH;
            case "ja" -> Locale.JAPANESE;
            case "ko" -> Locale.KOREAN;
            case "fr" -> Locale.FRENCH;
            case "de" -> Locale.GERMAN;
            default -> Locale.CHINA;
        };
    }

    /**
     * 中文数字格式化（支持 0 ~ 99999999）
     */
    private String formatChineseNumber(long number, boolean upper) {
        if (number == 0) {
            return upper ? "零元整" : "零";
        }
        if (number < 0) {
            return (upper ? "负" : "负") + formatChineseNumber(-number, upper);
        }

        char[] digits = upper ? CN_DIGITS : CN_DIGITS_SIMPLE;
        StringBuilder sb = new StringBuilder();
        String numStr = String.valueOf(number);
        int len = numStr.length();

        boolean needZero = false;
        for (int i = 0; i < len; i++) {
            int digit = numStr.charAt(i) - '0';
            int pos = len - i - 1; // 从右往左的位置
            int sectionPos = pos % 4;
            int sectionIdx = pos / 4;

            if (digit == 0) {
                needZero = true;
            } else {
                if (needZero && sb.length() > 0 && sb.charAt(sb.length() - 1) != '零') {
                    sb.append('零');
                }
                sb.append(digits[digit]);
                sb.append(CN_UNITS[sectionPos]);
                needZero = false;
            }

            // 每4位添加万/亿等单位
            if (sectionPos == 0 && sectionIdx > 0 && digit == 0) {
                // 检查该段是否全为零
                boolean allZero = true;
                for (int j = Math.max(0, i - 3); j <= i; j++) {
                    if (numStr.charAt(j) != '0') {
                        allZero = false;
                        break;
                    }
                }
                if (!allZero) {
                    // 移除末尾的零
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '零') {
                        sb.setLength(sb.length() - 1);
                    }
                    sb.append(CN_UNITS_BIG[sectionIdx]);
                }
                needZero = false;
            } else if (sectionPos == 0 && sectionIdx > 0) {
                sb.append(CN_UNITS_BIG[sectionIdx]);
            }
        }

        // 移除末尾的零
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '零') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * 罗马数字格式化（支持 1 ~ 3999）
     */
    private String formatRomanNumber(long number) {
        if (number <= 0) {
            return "错误: 罗马数字仅支持正整数（1 ~ 3999）";
        }
        if (number > 3999) {
            return "错误: 罗马数字最大支持 3999，当前值: " + number;
        }

        StringBuilder sb = new StringBuilder();
        int n = (int) number;
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (n >= ROMAN_VALUES[i]) {
                sb.append(ROMAN_SYMBOLS[i]);
                n -= ROMAN_VALUES[i];
            }
        }
        return sb.toString();
    }
}
