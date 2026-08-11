package com.umtle.umtleapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.util.StringUtils
import java.util.function.Supplier

/**
 * A browser SPA reads the raw token straight from the XSRF-TOKEN cookie and
 * echoes it back verbatim in a header. The default XorCsrfTokenRequestAttributeHandler
 * instead expects the BREACH-protected (masked) value it renders into server-side
 * views, so a raw cookie value always fails its comparison. Delegate to it for
 * cookie persistence, but resolve the submitted value straight from the header.
 */
class SpaCsrfTokenRequestHandler : CsrfTokenRequestAttributeHandler() {
    private val delegate = XorCsrfTokenRequestAttributeHandler()

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        csrfToken: Supplier<CsrfToken>,
    ) {
        delegate.handle(request, response, csrfToken)
    }

    override fun resolveCsrfTokenValue(
        request: HttpServletRequest,
        csrfToken: CsrfToken,
    ): String? {
        val headerValue = request.getHeader(csrfToken.headerName)
        return if (StringUtils.hasText(headerValue)) headerValue else delegate.resolveCsrfTokenValue(request, csrfToken)
    }
}
