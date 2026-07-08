package com.zmz.sdd.fullstack.core.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.zmz.sdd.fullstack.core.common.ErrorCode;
import com.zmz.sdd.fullstack.core.common.Result;
import com.zmz.sdd.fullstack.core.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §4 BR-15 + §1.6]
 * BE-08:校验 Authorization Bearer JWT;失败统一 3003。
 * 公开路径 /api/login 由 SecurityConfig 放行,本 Filter 直接 pass-through。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTH = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isPublic(request)) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(AUTH);
        if (header == null || !header.startsWith(BEARER)) {
            writeUnauthorized(response);
            return;
        }
        String token = header.substring(BEARER.length()).trim();
        Optional<JWTClaimsSet> claims = jwtService.verify(token);
        if (claims.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        String sub;
        String username;
        try {
            sub = claims.get().getSubject();
            username = claims.get().getStringClaim("username");
        } catch (java.text.ParseException pe) {
            writeUnauthorized(response);
            return;
        }
        if (sub == null) {
            writeUnauthorized(response);
            return;
        }
        try {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(sub, null, Collections.emptyList());
            auth.setDetails(username);
            SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                request.setAttribute("appUserId", Long.parseLong(sub));
            } catch (NumberFormatException e) {
                writeUnauthorized(response);
                return;
            }
            request.setAttribute("username", username);
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isPublic(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri.endsWith("/api/login")
                || uri.contains("/swagger-ui")
                || uri.contains("/v3/api-docs")
                || uri.endsWith("/actuator/health");
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Result<Void> body = Result.fail(ErrorCode.AUTH_REQUIRED, ErrorCode.AUTH_REQUIRED.getDefaultMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
