package com.flashsale.ordersystem.shared.filter;

import com.flashsale.ordersystem.shared.service.MetricsService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private  final MetricsService metricsService;
    private final ProxyManager<String> proxyManager;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        boolean isPurchaseEndpoint =
                method.equals("POST") &&
                        uri.matches("/api/v1/sales/\\d+/purchase");

        if (!isPurchaseEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = resolveUserId(request);

//        Authentication authentication =
//                SecurityContextHolder.getContext().getAuthentication();
//        String userId = authentication.getName();


        String bucketKey = "rate-limit:" + userId;

        Bucket bucket = proxyManager.builder()
                .build(
                        bucketKey,
                        this::createBucketConfiguration
                );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            metricsService.incrementAllowedRequests();
            log.info(
                    "Request allowed for user={}, remainingTokens={}",
                    userId,
                    probe.getRemainingTokens()
            );

            filterChain.doFilter(request, response);
            return;
        }

        metricsService.incrementBlockedRequests();
        log.warn(
                "Rate limit exceeded for user={}, remainingTokens={}",
                userId,
                probe.getRemainingTokens()
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.setHeader("Retry-After", "10");

        response.getWriter().write("""
                {
                  "error": "rate_limit_exceeded"
                }
                """);
    }

    private BucketConfiguration createBucketConfiguration() {

        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofSeconds(10))
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

    private String resolveUserId(HttpServletRequest request) {
        return request.getHeader("X-USER-ID") != null
                ? request.getHeader("X-USER-ID")
                : "anonymous";
    }

//    private String resolveUserId(HttpServletRequest request) {
//
//        Authentication authentication =
//                SecurityContextHolder.getContext().getAuthentication();
//
//        return authentication.getName();
//    }
}