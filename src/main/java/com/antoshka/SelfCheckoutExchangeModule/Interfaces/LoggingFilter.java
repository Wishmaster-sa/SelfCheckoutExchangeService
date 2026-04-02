/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.antoshka.SelfCheckoutExchangeModule.Interfaces;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 *
 * @author sa
 */
/**
 * Фильтр для логирования входящих и исходящих HTTP запросов
 * Логирует тело запроса и ответа
 */
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Value("${app.logging.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();

        filterChain.doFilter(req, res);

        long time = System.currentTimeMillis() - start;

        String requestBody = new String(req.getContentAsByteArray(), StandardCharsets.UTF_8);
        String responseBody = new String(res.getContentAsByteArray(), StandardCharsets.UTF_8);

        log.info("REQ {} {} body={}", request.getMethod(), request.getRequestURI(), requestBody);
        log.info("RES {} time={}ms body={}", response.getStatus(), time, responseBody);

        res.copyBodyToResponse();
    }
}