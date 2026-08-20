package com.sonali.urlshortener.config;

import com.sonali.urlshortener.exception.RateLimitExceededException;
import com.sonali.urlshortener.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = resolveClientIp(request);

        if (!rateLimiterService.isAllowed(clientIp)) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded: max " + rateLimiterService.getMaxRequests()
                            + " requests per " + rateLimiterService.getWindowSeconds() + " seconds");
        }

        return true;
    }

    // Behind a proxy/load balancer, the real client IP is forwarded in
    // X-Forwarded-For; fall back to the direct remote address otherwise.
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}