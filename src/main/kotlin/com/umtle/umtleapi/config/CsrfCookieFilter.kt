package com.umtle.umtleapi.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Spring Security defers CSRF token generation until something reads it (e.g. a
 * server-rendered view). A pure REST client never triggers that read, so the
 * XSRF-TOKEN cookie is never issued without this filter forcing the read.
 */
class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        (request.getAttribute(CsrfToken::class.java.name) as CsrfToken?)?.token
        filterChain.doFilter(request, response)
    }
}
