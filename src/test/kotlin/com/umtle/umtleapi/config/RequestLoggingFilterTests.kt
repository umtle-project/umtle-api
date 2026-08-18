package com.umtle.umtleapi.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.umtle.umtleapi.TestcontainersConfiguration
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.core.io.FileSystemResource
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class RequestLoggingFilterTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var environment: Environment

    private lateinit var logger: Logger
    private lateinit var appender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun attachLogAppender() {
        logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java) as Logger
        appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
    }

    @AfterEach
    fun detachLogAppender() {
        logger.detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `health request receives trace id and access log with same trace id`() {
        val result =
            mockMvc
                .perform(get("/actuator/health"))
                .andExpect(status().isOk)
                .andReturn()

        val traceId = result.response.getHeader(RequestLoggingFilter.TRACE_ID_HEADER)
        assertNotNull(traceId)

        val accessLog = singleAccessLog("GET /actuator/health -> 200 (")
        assertEquals(traceId, accessLog.mdcPropertyMap[RequestLoggingFilter.TRACE_ID])
    }

    @Test
    fun `protected request rejected by security still receives trace id and access log`() {
        val result =
            mockMvc
                .perform(get("/api/v1/students"))
                .andExpect(status().isUnauthorized)
                .andReturn()

        val traceId = result.response.getHeader(RequestLoggingFilter.TRACE_ID_HEADER)
        assertNotNull(traceId)

        val accessLog = singleAccessLog("GET /api/v1/students -> 401 (")
        assertEquals(traceId, accessLog.mdcPropertyMap[RequestLoggingFilter.TRACE_ID])
    }

    @Test
    fun `two requests receive different trace ids`() {
        val first =
            mockMvc
                .perform(get("/actuator/health"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .getHeader(RequestLoggingFilter.TRACE_ID_HEADER)

        val second =
            mockMvc
                .perform(get("/actuator/health"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .getHeader(RequestLoggingFilter.TRACE_ID_HEADER)

        assertNotNull(first)
        assertNotNull(second)
        assertNotEquals(first, second)
    }

    @Test
    fun `not found domain exception is recorded as 404 access log`() {
        val session = adminSession()
        appender.list.clear()

        val result =
            mockMvc
                .perform(get("/api/v1/students/999999999").session(session))
                .andExpect(status().isNotFound)
                .andReturn()

        val traceId = result.response.getHeader(RequestLoggingFilter.TRACE_ID_HEADER)
        assertNotNull(traceId)

        val accessLog = singleAccessLog("GET /api/v1/students/999999999 -> 404 (")
        assertEquals(traceId, accessLog.mdcPropertyMap[RequestLoggingFilter.TRACE_ID])
    }

    @Test
    fun `test profile suppresses hibernate sql debug logging`() {
        assertEquals("warn", environment.getProperty("logging.level.org.hibernate.SQL"))
        assertEquals("%5p [%X{traceId:-}]", environment.getProperty("logging.pattern.level"))

        val sqlLogger = LoggerFactory.getLogger("org.hibernate.SQL") as Logger
        assertFalse(sqlLogger.isDebugEnabled)
        assertTrue(sqlLogger.isEnabledFor(Level.WARN))
    }

    @Test
    fun `main logging config enables formatted sql without bind parameter logging`() {
        val properties =
            YamlPropertiesFactoryBean()
                .apply { setResources(FileSystemResource("src/main/resources/application.yml")) }
                .getObject()

        assertEquals("debug", properties?.getProperty("logging.level.org.hibernate.SQL"))
        assertEquals("true", properties?.getProperty("spring.jpa.properties.hibernate.format_sql"))
        assertEquals("%5p [%X{traceId:-}]", properties?.getProperty("logging.pattern.level"))
        assertFalse(properties?.containsKey("logging.level.org.hibernate.orm.jdbc.bind") ?: true)
    }

    private fun singleAccessLog(messagePrefix: String): ILoggingEvent =
        appender.list.single { event ->
            event.level == Level.INFO && event.formattedMessage.startsWith(messagePrefix)
        }

    private fun adminSession(): MockHttpSession =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf("loginId" to "test-admin", "password" to "test-admin-password"),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andReturn()
            .request
            .session
            .toMockHttpSession()

    private fun HttpSession?.toMockHttpSession(): MockHttpSession = this as MockHttpSession
}
