package com.umtle.umtleapi.homework

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
class HomeworkApiTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `teacher can assign student and lesson based homework list get update and delete`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val classId = createClass("숙제 반 ${shortId()}", adminSession)
        val studentId = createStudent("숙제 학생 ${shortId()}", adminSession)
        assignStudent(classId, studentId, adminSession)
        val lessonId = createLesson(classId, adminSession)

        val studentHomeworkId = assignHomework(studentId, null, "학생 단위 숙제", teacherSession)
        val lessonHomeworkId = assignHomework(studentId, lessonId, "수업 단위 숙제", teacherSession)

        mockMvc
            .perform(get("/api/v1/homeworks").session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").exists())

        mockMvc
            .perform(get("/api/v1/homeworks?studentId=$studentId").session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].studentId").value(studentId))

        mockMvc
            .perform(get("/api/v1/homeworks/$lessonHomeworkId").session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lessonId").value(lessonId))
            .andExpect(jsonPath("$.status").value("ASSIGNED"))

        mockMvc
            .perform(
                patch("/api/v1/homeworks/$lessonHomeworkId")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("title" to "수정된 숙제", "status" to "SUBMITTED"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("수정된 숙제"))
            .andExpect(jsonPath("$.status").value("SUBMITTED"))

        mockMvc
            .perform(delete("/api/v1/homeworks/$studentHomeworkId").with(csrf()).session(teacherSession))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(get("/api/v1/homeworks/$studentHomeworkId").session(teacherSession))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `lesson based homework rejects unassigned student`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val classId = createClass("숙제 배정 반 ${shortId()}", adminSession)
        val unassignedStudentId = createStudent("미배정 숙제 학생 ${shortId()}", adminSession)
        val lessonId = createLesson(classId, adminSession)

        mockMvc
            .perform(
                post("/api/v1/homeworks")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf("studentId" to unassignedStudentId, "lessonId" to lessonId, "title" to "수업 숙제"),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `missing student lesson and homework ids return 404`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val studentId = createStudent("숙제 404 학생 ${shortId()}", adminSession)

        mockMvc
            .perform(
                post("/api/v1/homeworks")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to 999999999L, "title" to "없는 학생 숙제"))),
            ).andExpect(status().isNotFound)

        mockMvc
            .perform(
                post("/api/v1/homeworks")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(mapOf("studentId" to studentId, "lessonId" to 999999999L, "title" to "없는 수업 숙제")),
                    ),
            ).andExpect(status().isNotFound)

        mockMvc
            .perform(get("/api/v1/homeworks/999999999").session(teacherSession))
            .andExpect(status().isNotFound)

        mockMvc
            .perform(delete("/api/v1/homeworks/999999999").with(csrf()).session(teacherSession))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `invalid homework requests return 400`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val studentId = createStudent("숙제 검증 학생 ${shortId()}", adminSession)
        val homeworkId = assignHomework(studentId, null, "검증 숙제", teacherSession)

        mockMvc
            .perform(
                post("/api/v1/homeworks")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId, "title" to ""))),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                post("/api/v1/homeworks")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId, "title" to "가".repeat(101)))),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                patch("/api/v1/homeworks/$homeworkId")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(emptyMap<String, String>())),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                patch("/api/v1/homeworks/$homeworkId")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("title" to " "))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `teacher role is required for homework api`() {
        val adminSession = adminSession()

        mockMvc
            .perform(get("/api/v1/homeworks").session(adminSession))
            .andExpect(status().isForbidden)

        mockMvc
            .perform(
                post("/api/v1/homeworks")
                    .with(csrf())
                    .session(adminSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to 1L, "title" to "관리자 숙제"))),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `unauthenticated access to homework returns 401`() {
        mockMvc
            .perform(get("/api/v1/homeworks"))
            .andExpect(status().isUnauthorized)
    }

    private fun adminSession(): MockHttpSession = loginSession("test-admin", "test-admin-password")

    private fun teacherSession(adminSession: MockHttpSession): MockHttpSession {
        val loginId = "teacher-homework-${shortId()}"
        createUser(loginId, "teacher-password", listOf("TEACHER"), adminSession)
        return loginSession(loginId, "teacher-password")
    }

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

    private fun assignStudent(
        classId: Long,
        studentId: Long,
        session: MockHttpSession,
    ) {
        mockMvc
            .perform(
                post("/api/v1/classes/$classId/students")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId))),
            ).andExpect(status().isOk)
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

    private fun assignHomework(
        studentId: Long,
        lessonId: Long?,
        title: String,
        session: MockHttpSession,
    ): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/homeworks")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(
                            objectMapper.writeValueAsString(mapOf("studentId" to studentId, "lessonId" to lessonId, "title" to title)),
                        ),
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
