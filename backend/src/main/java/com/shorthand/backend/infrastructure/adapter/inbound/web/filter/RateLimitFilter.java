package com.shorthand.backend.infrastructure.adapter.inbound.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shorthand.backend.infrastructure.adapter.inbound.web.exception.ErrorResponse;
import com.shorthand.backend.infrastructure.config.ShorthandProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> rateLimitScript;
    private final ShorthandProperties shorthandProperties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RedisTemplate<String, String> redisTemplate, RedisScript<Long> rateLimitScript, ShorthandProperties shorthandProperties, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
        this.shorthandProperties = shorthandProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (!path.startsWith("/api/v1/links")) {
            chain.doFilter(request, response);
            return;
        }

        String ipAddress = getClientIpAddress(httpRequest);
        String redisKey = shorthandProperties.rateLimiter().keyPrefix() + ":" + ipAddress;

        long nowInSeconds = Instant.now().getEpochSecond();
        long requestedTokens = 1;

        Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(redisKey),
                String.valueOf(shorthandProperties.rateLimiter().capacity()),
                String.valueOf(shorthandProperties.rateLimiter().refillRate()),
                String.valueOf(nowInSeconds),
                String.valueOf(requestedTokens)
        );

        if (result != 1) {
            ErrorResponse errorResponse = new ErrorResponse(
                    Instant.now(),
                    429,
                    "Too Many Requests",
                    "Rate limit exceeded. Please try again later."
            );
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        chain.doFilter(request, response);
    }

    private static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        } else {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
