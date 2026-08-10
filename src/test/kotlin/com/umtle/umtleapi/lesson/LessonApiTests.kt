package com.umtle.umtleapi.lesson

import com.umtle.umtleapi.TestcontainersConfiguration
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class LessonApiTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `admin can create read list cancel and reject invalid lesson transitions`() {
        val session = adminSession()
        val classId = createClass("수업용 반", session)
        val lessonId = createLesson(classId, session)

        mockMvc
            .perform(get("/api/v1/lessons/$lessonId").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.classId").value(classId))
            .andExpect(jsonPath("$.status").value("SCHEDULED"))

        mockMvc
            .perform(get("/api/v1/lessons").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").exists())

        mockMvc
            .perform(post("/api/v1/lessons/$lessonId/cancel").with(csrf()).session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))

        mockMvc
            .perform(post("/api/v1/lessons/$lessonId/complete").with(csrf()).session(session))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `admin can complete a scheduled lesson`() {
        val session = adminSession()
        val classId = createClass("완료용 반", session)
        val lessonId = createLesson(classId, session)

        mockMvc
            .perform(post("/api/v1/lessons/$lessonId/complete").with(csrf()).session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETED"))
    }

    @Test
    fun `creating lesson for missing class returns 404`() {
        mockMvc
            .perform(
                post("/api/v1/lessons")
                    .with(csrf())
                    .session(adminSession())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("classId" to 999999999L))),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `missing lesson returns 404`() {
        mockMvc
            .perform(get("/api/v1/lessons/999999999").session(adminSession()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `unauthenticated access to lessons returns 401`() {
        mockMvc
            .perform(get("/api/v1/lessons"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `teacher access to lessons is forbidden`() {
        val adminSession = adminSession()
        val teacherLoginId = "teacher-lesson-${shortId()}"
        createUser(teacherLoginId, "teacher-password", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")

        mockMvc
            .perform(get("/api/v1/lessons").session(teacherSession))
            .andExpect(status().isForbidden)
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
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("loginId" to loginId, "password" to password))),
            ).andExpect(status().isOk)
            .andReturn()
            .request
            .session
            .toMockHttpSession()

    private fun createClass(
        name: String,
        session: MockHttpSession,
    ): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/classes")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("name" to name))),
                ).andExpect(status().isCreated)
                .andReturn()

        return response.readId()
    }

    private fun createLesson(
        classId: Long,
        session: MockHttpSession,
    ): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/lessons")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("classId" to classId))),
                ).andExpect(status().isCreated)
                .andReturn()

        return response.readId()
    }

    private fun createUser(
        loginId: String,
        password: String,
        roles: List<String>,
        session: MockHttpSession,
    ): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/users")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("loginId" to loginId, "password" to password, "roles" to roles))),
                ).andExpect(status().isCreated)
                .andReturn()

        return response.readId()
    }

    private fun org.springframework.test.web.servlet.MvcResult.readId(): Long =
        objectMapper
            .readTree(response.contentAsString)
            .get("id")
            .asLong()

    private fun shortId(): String = UUID.randomUUID().toString().take(8)

    private fun HttpSession?.toMockHttpSession(): MockHttpSession = this as MockHttpSession
}
