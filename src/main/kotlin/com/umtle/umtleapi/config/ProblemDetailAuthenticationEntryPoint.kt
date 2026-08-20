package com.umtle.umtleapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ProblemDetailAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Unauthorized")
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        objectMapper.writeValue(response.outputStream, problemDetail)
    }
}
