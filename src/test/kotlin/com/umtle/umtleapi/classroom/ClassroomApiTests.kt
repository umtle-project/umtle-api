package com.umtle.umtleapi.classroom

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ClassroomApiTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `admin can manage classes and assignments`() {
        val session = adminSession()
        val classId = createClass("중등 수학", session)
        val studentId = createStudent("배정 학생", session)
        val teacherId = createUser("teacher-${shortId()}", "teacher-password", listOf("TEACHER"), session)

        mockMvc
            .perform(
                patch("/api/v1/classes/$classId")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "고등 수학"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("고등 수학"))

        mockMvc
            .perform(
                post("/api/v1/classes/$classId/students")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.studentIds[0]").value(studentId))

        mockMvc
            .perform(
                post("/api/v1/classes/$classId/teachers")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("teacherId" to teacherId))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.teacherIds[0]").value(teacherId))

        mockMvc
            .perform(delete("/api/v1/classes/$classId/students/$studentId").with(csrf()).session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.studentIds").isEmpty)

        mockMvc
            .perform(delete("/api/v1/classes/$classId/teachers/$teacherId").with(csrf()).session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.teacherIds").isEmpty)

        mockMvc
            .perform(post("/api/v1/classes/$classId/deactivate").with(csrf()).session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("INACTIVE"))

        mockMvc
            .perform(get("/api/v1/classes/$classId").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("고등 수학"))

        mockMvc
            .perform(get("/api/v1/classes").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").exists())
    }

    @Test
    fun `missing class returns 404`() {
        mockMvc
            .perform(get("/api/v1/classes/999999999").session(adminSession()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `assigning missing student returns 404`() {
        val session = adminSession()
        val classId = createClass("중등 영어", session)

        mockMvc
            .perform(
                post("/api/v1/classes/$classId/students")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to 999999999L))),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `assigning a non teacher user returns 400`() {
        val session = adminSession()
        val classId = createClass("중등 과학", session)
        val studentRoleUserId = createUser("student-role-${shortId()}", "student-password", listOf("STUDENT"), session)

        mockMvc
            .perform(
                post("/api/v1/classes/$classId/teachers")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("teacherId" to studentRoleUserId))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `unauthenticated access to classes returns 401`() {
        mockMvc
            .perform(get("/api/v1/classes"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `teacher access to classes is forbidden`() {
        val adminSession = adminSession()
        val teacherLoginId = "teacher-class-${shortId()}"
        createUser(teacherLoginId, "teacher-password", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")

        mockMvc
            .perform(get("/api/v1/classes").session(teacherSession))
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

    private fun createStudent(
        name: String,
        session: MockHttpSession,
    ): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/students")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("name" to name))),
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
