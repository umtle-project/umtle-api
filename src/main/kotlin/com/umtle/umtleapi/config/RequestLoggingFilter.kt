package com.umtle.umtleapi.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()

        MDC.put(TRACE_ID, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)

        try {
            filterChain.doFilter(request, response)
        } catch (throwable: Throwable) {
            log.error("Unhandled exception during request", throwable)
            throw throwable
        } finally {
            val durationMs = System.currentTimeMillis() - startedAt
            log.info("{} {} -> {} ({} ms)", request.method, request.requestURI, response.status, durationMs)
            MDC.remove(TRACE_ID)
        }
    }

    companion object {
        const val TRACE_ID = "traceId"
        const val TRACE_ID_HEADER = "X-Trace-Id"
    }
}
