package com.example.mcp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String method = request.getMethod();
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();

        String fullUrl = queryString != null ? requestUri + "?" + queryString : requestUri;

        log.info("API Request - Method: {}, Path: {}", method, fullUrl);

        filterChain.doFilter(request, response);
    }
}