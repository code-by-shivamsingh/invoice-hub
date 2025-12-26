
package com.jokati.invoice.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_APPLICATION = "X-Application-Name";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String applicationName = request.getHeader(HEADER_APPLICATION);
        String incomingTraceId = request.getHeader(HEADER_TRACE_ID);

        // Generate short trace id if missing (like "b3f8e5b2")
        String traceId = (incomingTraceId != null && !incomingTraceId.isBlank())
                ? incomingTraceId
                : UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Put in MDC for logging and handlers
        MDC.put("traceId", traceId);
        if (applicationName != null && !applicationName.isBlank()) {
            MDC.put("application", applicationName);
        } else {
            MDC.remove("application");
        }

        try {
            // Propagate to response headers
            response.setHeader(HEADER_TRACE_ID, traceId);
            if (applicationName != null && !applicationName.isBlank()) {
                response.setHeader(HEADER_APPLICATION, applicationName);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            MDC.remove("application");
        }
    }
}