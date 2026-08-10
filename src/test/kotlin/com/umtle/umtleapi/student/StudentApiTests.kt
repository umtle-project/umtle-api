package com.umtle.umtleapi.student

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class StudentApiTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `register, get, list, update and deactivate a student end to end`() {
        val session = adminSession()
        val registerResponse =
            mockMvc
                .perform(
                    post("/api/v1/students")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("name" to "홍길동"))),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()

        val studentId =
            objectMapper
                .readTree(registerResponse.response.contentAsString)
                .get("id")
                .asLong()

        mockMvc
            .perform(get("/api/v1/students/$studentId").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("홍길동"))

        mockMvc
            .perform(get("/api/v1/students").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").exists())

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "김철수"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("김철수"))

        mockMvc
            .perform(post("/api/v1/students/$studentId/deactivate").with(csrf()).session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("INACTIVE"))
    }

    @Test
    fun `getting a non-existent student returns 404`() {
        val session = adminSession()

        mockMvc
            .perform(get("/api/v1/students/999999999").session(session))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `registering with a blank name returns 400`() {
        val session = adminSession()

        mockMvc
            .perform(
                post("/api/v1/students")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to ""))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `registering with a name longer than the database column returns 400`() {
        val session = adminSession()

        mockMvc
            .perform(
                post("/api/v1/students")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "가".repeat(101)))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `updating with a name longer than the database column returns 400`() {
        val session = adminSession()
        val registerResponse =
            mockMvc
                .perform(
                    post("/api/v1/students")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("name" to "홍길동"))),
                ).andExpect(status().isCreated)
                .andReturn()

        val studentId =
            objectMapper
                .readTree(registerResponse.response.contentAsString)
                .get("id")
                .asLong()

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "가".repeat(101)))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `unauthenticated access to students returns 401`() {
        mockMvc
            .perform(get("/api/v1/students"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `teacher can read students but cannot write students`() {
        val adminSession = adminSession()
        createUser("teacher-student-api-test", "teacher-password", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession("teacher-student-api-test", "teacher-password")

        mockMvc
            .perform(get("/api/v1/students").session(teacherSession))
            .andExpect(status().isOk)

        mockMvc
            .perform(
                post("/api/v1/students")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "권한없음"))),
            ).andExpect(status().isForbidden)
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

    private fun createUser(
        loginId: String,
        password: String,
        roles: List<String>,
        session: MockHttpSession,
    ) {
        mockMvc
            .perform(
                post("/api/v1/users")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("loginId" to loginId, "password" to password, "roles" to roles))),
            ).andExpect(status().isCreated)
    }

    private fun HttpSession?.toMockHttpSession(): MockHttpSession = this as MockHttpSession
}
