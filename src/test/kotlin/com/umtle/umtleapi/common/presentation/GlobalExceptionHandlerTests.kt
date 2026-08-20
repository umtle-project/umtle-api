package com.umtle.umtleapi.common.presentation

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.umtle.umtleapi.TestcontainersConfiguration
import jakarta.servlet.http.HttpSession
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, GlobalExceptionHandlerTests.TestExceptionController::class)
class GlobalExceptionHandlerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var logger: Logger
    private lateinit var appender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun attachLogAppender() {
        logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java) as Logger
        appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
    }

    @AfterEach
    fun detachLogAppender() {
        logger.detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `domain exceptions keep their previous problem detail status and body`() {
        val session = adminSession()

        mockMvc
            .perform(get("/api/v1/students/999999999").session(session))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Student not found: 999999999"))

        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "loginId" to "test-admin",
                                "password" to "teacher-password",
                                "name" to "중복 사용자",
                                "role" to "TEACHER",
                            ),
                        ),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Duplicate loginId: test-admin"))
    }

    @Test
    fun `unhandled exception returns fixed 500 problem detail and is logged`() {
        val session = adminSession()

        mockMvc
            .perform(get("/test/exceptions/unhandled").session(session))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value("Internal server error"))
            .andExpect(jsonPath("$.detail").value(not(containsString("Sensitive failure detail"))))

        assertTrue(
            appender.list.any { event ->
                event.level == Level.ERROR &&
                    event.formattedMessage == "Unhandled exception during request" &&
                    event.throwableProxy?.className == RuntimeException::class.qualifiedName
            },
        )
    }

    @Test
    fun `validation failure remains Spring MVC 400 problem detail`() {
        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "loginId" to "",
                                "password" to "teacher-password",
                                "name" to "검증 실패",
                                "role" to "TEACHER",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value(not("Internal server error")))
    }

    @Test
    fun `unauthenticated protected request returns 401 problem detail`() {
        mockMvc
            .perform(get("/api/v1/students"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Unauthorized"))
    }

    @Test
    fun `role mismatch returns 403 problem detail`() {
        val adminSession = adminSession()
        val teacherLoginId = "exception-teacher-${shortId()}"
        createUser(teacherLoginId, adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")

        mockMvc
            .perform(
                post("/api/v1/students")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("name" to "권한 없음"))),
            ).andExpect(status().isForbidden)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("Forbidden"))
    }

    @Test
    fun `service access denied exception returns 403 problem detail`() {
        val adminSession = adminSession()
        val teacherLoginId = "approval-denied-teacher-${shortId()}"
        createUser(teacherLoginId, adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")
        val pendingTeacherId = signupTeacher("pending-denied-teacher-${shortId()}")

        mockMvc
            .perform(post("/api/v1/users/$pendingTeacherId/approve").with(csrf()).session(teacherSession))
            .andExpect(status().isForbidden)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("Forbidden"))
    }

    private fun adminSession(): MockHttpSession = loginSession("test-admin", "test-admin-password")

    private fun loginSession(
        loginId: String,
        password: String,
    ): MockHttpSession =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("loginId" to loginId, "password" to password))),
            ).andExpect(status().isOk)
            .andReturn()
            .request
            .session
            .toMockHttpSession()

    private fun createUser(
        loginId: String,
        session: MockHttpSession,
    ): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/users")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "loginId" to loginId,
                                    "password" to "teacher-password",
                                    "name" to "예외 선생님",
                                    "roles" to listOf("TEACHER"),
                                ),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andReturn()
        return objectMapper.readTree(response.response.contentAsString).get("id").asLong()
    }

    private fun signupTeacher(loginId: String): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "loginId" to loginId,
                                    "password" to "teacher-password",
                                    "name" to "대기 선생님",
                                    "role" to "TEACHER",
                                ),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andReturn()
        return objectMapper.readTree(response.response.contentAsString).get("id").asLong()
    }

    private fun HttpSession?.toMockHttpSession(): MockHttpSession = this as MockHttpSession

    private fun shortId(): String = UUID.randomUUID().toString().take(8)

    @RestController
    @RequestMapping("/test/exceptions")
    class TestExceptionController {
        @GetMapping("/unhandled")
        fun unhandled(): Nothing = throw RuntimeException("Sensitive failure detail")

        @GetMapping("/access-denied")
        fun accessDenied(): Nothing = throw AccessDeniedException("Access denied")
    }
}
