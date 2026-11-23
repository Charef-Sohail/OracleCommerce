package com.enset.oraclecommerce.oracle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Wraps each page request: starts a SQL trace, and on completion publishes an event
 * if (and only if) the request really sent SQL to Oracle.
 */
@Component
public class OracleTraceInterceptor implements HandlerInterceptor {

    private static final String START = OracleTraceInterceptor.class.getName() + ".start";

    private final OracleEventBuffer buffer;

    public OracleTraceInterceptor(OracleEventBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequestTrace.begin();
        request.setAttribute(START, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        List<OracleEvent.Stmt> statements = RequestTrace.end();
        if (statements.isEmpty()) return;   // validation error, JdbcTemplate-only page...: nothing hit Hibernate

        Object start = request.getAttribute(START);
        long ms = start instanceof Long s ? (System.nanoTime() - s) / 1_000_000 : 0;
        buffer.publish(request.getMethod(), request.getRequestURI(), response.getStatus(), ms, statements);
    }
}
