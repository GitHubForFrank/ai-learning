package com.gitgui.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON 工具
 * <p>封装 Jackson {@link ObjectMapper}，用于设置值、审计详情、操作日志参数等 JSON 序列化。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class JsonUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    /** 单例 ObjectMapper（线程安全） */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private JsonUtil() {
        // 工具类禁止实例化
    }

    /**
     * 获取全局 ObjectMapper。
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * 序列化对象为 JSON 字符串。
     *
     * @param obj 待序列化对象
     * @return JSON 字符串；序列化失败返回空字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON 序列化失败：{}", obj.getClass().getName(), e);
            return "";
        }
    }

    /**
     * 反序列化 JSON 字符串为指定类型。
     *
     * @param json   JSON 字符串
     * @param clazz  目标类型
     * @param <T>    泛型
     * @return 反序列化对象；失败返回 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("JSON 反序列化失败：type={}, json={}", clazz.getName(), json, e);
            return null;
        }
    }
}
