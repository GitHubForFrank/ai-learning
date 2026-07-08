package com.zmz.sdd.fullstack.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §4 BR-13 + conventions/monitoring-conventions.md §5.2]
 * BE-09:解析 W3C traceparent,缺失时兜底生成,写入 Logback MDC.trace_id
 * Order 必须 < JwtFilter,确保后续日志已有 trace_id
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceparentFilter extends OncePerRequestFilter {

    public static final String HEADER = "traceparent";
    public static final String MDC_TRACE_ID = "trace_id";

    // W3C: 00-{32 hex}-{16 hex}-{2 hex}
    private static final Pattern W3C_PATTERN =
            Pattern.compile("^[0-9a-f]{2}-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tp = request.getHeader(HEADER);
        boolean generated = false;
        if (tp == null || !W3C_PATTERN.matcher(tp).matches()) {
            tp = generate();
            generated = true;
        }
        String traceId = extractTraceId(tp);
        try {
            MDC.put(MDC_TRACE_ID, traceId);
            request.setAttribute(HEADER, tp);
            response.setHeader(HEADER, tp);
            if (generated) {
                log.info("traceparent missing or malformed; generated new one path={} ua={}",
                        request.getRequestURI(), request.getHeader("User-Agent"));
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private static String generate() {
        // 00-<32 hex traceId>-<16 hex spanId>-01
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String spanId  = traceId.substring(0, 16);
        return "00-" + traceId + "-" + spanId + "-01";
    }

    private static String extractTraceId(String traceparent) {
        return traceparent.split("-")[1];
    }
}
