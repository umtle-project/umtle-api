package com.umtle.umtleapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val problemDetailAuthenticationEntryPoint: ProblemDetailAuthenticationEntryPoint,
    private val problemDetailAccessDeniedHandler: ProblemDetailAccessDeniedHandler,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors {
                configurationSource = corsConfigurationSource()
            }
            authorizeHttpRequests {
                authorize(HttpMethod.POST, "/api/v1/auth/login", permitAll)
                authorize(HttpMethod.POST, "/api/v1/auth/signup", permitAll)
                authorize("/actuator/health", permitAll)
                authorize(HttpMethod.GET, "/api/v1/students/search", permitAll)
                authorize(HttpMethod.GET, "/api/v1/users/pending", hasAnyRole("ADMIN", "TEACHER"))
                authorize(HttpMethod.POST, "/api/v1/users/*/approve", hasAnyRole("ADMIN", "TEACHER"))
                authorize(HttpMethod.POST, "/api/v1/users/*/reject", hasAnyRole("ADMIN", "TEACHER"))
                authorize(HttpMethod.POST, "/api/v1/lessons/*/attendances", hasRole("TEACHER"))
                authorize(HttpMethod.GET, "/api/v1/lessons/*/attendances", hasAnyRole("ADMIN", "TEACHER"))
                authorize(HttpMethod.GET, "/api/v1/attendances", hasAnyRole("ADMIN", "TEACHER"))
                authorize(HttpMethod.GET, "/api/v1/attendances/**", hasAnyRole("ADMIN", "TEACHER"))
                authorize(HttpMethod.PATCH, "/api/v1/attendances/**", hasRole("TEACHER"))
                authorize("/api/v1/homeworks", hasRole("TEACHER"))
                authorize("/api/v1/homeworks/**", hasRole("TEACHER"))
                authorize(HttpMethod.POST, "/api/v1/learning-records", hasRole("TEACHER"))
                authorize(HttpMethod.PATCH, "/api/v1/learning-records/**", hasRole("TEACHER"))
                authorize(HttpMethod.GET, "/api/v1/learning-records", hasAnyRole("ADMIN", "TEACHER"))
                authorize(HttpMethod.GET, "/api/v1/learning-records/**", hasAnyRole("ADMIN", "TEACHER"))
                authorize("/api/v1/students/**", hasAnyRole("ADMIN", "TEACHER"))
                authorize("/api/v1/users/**", hasRole("ADMIN"))
                authorize("/api/v1/classes/**", hasRole("ADMIN"))
                authorize("/api/v1/lessons/**", hasRole("ADMIN"))
                authorize(anyRequest, authenticated)
            }
            csrf {
                csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()
                csrfTokenRequestHandler = SpaCsrfTokenRequestHandler()
            }
            exceptionHandling {
                authenticationEntryPoint = problemDetailAuthenticationEntryPoint
                accessDeniedHandler = problemDetailAccessDeniedHandler
            }
            formLogin { disable() }
            httpBasic { disable() }
        }
        http.addFilterAfter(CsrfCookieFilter(), CsrfFilter::class.java)
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOrigins = listOf("http://localhost:3000")
                allowedMethods = listOf("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Content-Type", "X-XSRF-TOKEN")
                allowCredentials = true
            }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
