package com.umtle.umtleapi.learningrecord

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
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class LearningRecordApiTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `teacher can record list get and update learning records`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val studentId = createStudent("학습기록 학생 ${shortId()}", adminSession)
        val otherStudentId = createStudent("학습기록 다른 학생 ${shortId()}", adminSession)
        val firstRecordId = recordLearningRecord(studentId, "첫 기록", "첫 내용", teacherSession)
        val secondRecordId = recordLearningRecord(studentId, "둘째 기록", "둘째 내용", teacherSession)
        recordLearningRecord(otherStudentId, "다른 학생 기록", "다른 학생 내용", teacherSession)

        mockMvc
            .perform(get("/api/v1/learning-records?studentId=$studentId").session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(secondRecordId))
            .andExpect(jsonPath("$[0].studentId").value(studentId))
            .andExpect(jsonPath("$[0].title").value("둘째 기록"))
            .andExpect(jsonPath("$[0].createdAt").exists())
            .andExpect(jsonPath("$[0].no").value(2))
            .andExpect(jsonPath("$[1].id").value(firstRecordId))
            .andExpect(jsonPath("$[1].createdAt").exists())
            .andExpect(jsonPath("$[1].no").value(1))
            .andExpect(jsonPath("$[?(@.studentId == '$otherStudentId')]").doesNotExist())

        mockMvc
            .perform(get("/api/v1/learning-records/$secondRecordId").session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("둘째 기록"))
            .andExpect(jsonPath("$.content").value("둘째 내용"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.no").doesNotExist())

        mockMvc
            .perform(
                patch("/api/v1/learning-records/$secondRecordId")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("title" to "수정 기록", "content" to "수정 내용"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("수정 기록"))
            .andExpect(jsonPath("$.content").value("수정 내용"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.no").doesNotExist())
    }

    @Test
    fun `admin can list and get learning records`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val studentId = createStudent("관리자 조회 학생 ${shortId()}", adminSession)
        val recordId = recordLearningRecord(studentId, "관리자 조회 기록", "관리자 조회 내용", teacherSession)

        mockMvc
            .perform(get("/api/v1/learning-records?studentId=$studentId").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(recordId))

        mockMvc
            .perform(get("/api/v1/learning-records/$recordId").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(recordId))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.no").doesNotExist())
    }

    @Test
    fun `missing student record and query parameter failures return documented statuses`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)

        mockMvc
            .perform(
                post("/api/v1/learning-records")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf("studentId" to 999999999L, "title" to "없는 학생 기록", "content" to "없는 학생 내용"),
                        ),
                    ),
            ).andExpect(status().isNotFound)

        mockMvc
            .perform(get("/api/v1/learning-records?studentId=999999999").session(teacherSession))
            .andExpect(status().isNotFound)

        mockMvc
            .perform(get("/api/v1/learning-records/999999999").session(teacherSession))
            .andExpect(status().isNotFound)

        mockMvc
            .perform(
                patch("/api/v1/learning-records/999999999")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("title" to "없는 기록", "content" to "없는 내용"))),
            ).andExpect(status().isNotFound)

        mockMvc
            .perform(get("/api/v1/learning-records").session(adminSession))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `invalid learning record requests return 400`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val studentId = createStudent("학습기록 검증 학생 ${shortId()}", adminSession)
        val recordId = recordLearningRecord(studentId, "검증 기록", "검증 내용", teacherSession)

        mockMvc
            .perform(
                post("/api/v1/learning-records")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId, "title" to "", "content" to "내용"))),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                post("/api/v1/learning-records")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId, "title" to "제목", "content" to ""))),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                post("/api/v1/learning-records")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf("studentId" to studentId, "title" to "가".repeat(101), "content" to "내용"),
                        ),
                    ),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                post("/api/v1/learning-records")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf("studentId" to studentId, "title" to "제목", "content" to "가".repeat(2001)),
                        ),
                    ),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                patch("/api/v1/learning-records/$recordId")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("title" to "제목만 있음"))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `only teacher can create or update and only admin or teacher can read learning records`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val studentSession = userSession("student-learning-record-${shortId()}", "student-password", listOf("STUDENT"), adminSession)
        val parentSession = userSession("parent-learning-record-${shortId()}", "parent-password", listOf("PARENT"), adminSession)
        val studentId = createStudent("권한 학습기록 학생 ${shortId()}", adminSession)
        val recordId = recordLearningRecord(studentId, "권한 기록", "권한 내용", teacherSession)

        mockMvc
            .perform(
                post("/api/v1/learning-records")
                    .with(csrf())
                    .session(adminSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId, "title" to "관리자 기록", "content" to "관리자 내용"))),
            ).andExpect(status().isForbidden)

        mockMvc
            .perform(
                patch("/api/v1/learning-records/$recordId")
                    .with(csrf())
                    .session(adminSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("title" to "관리자 수정", "content" to "관리자 내용"))),
            ).andExpect(status().isForbidden)

        mockMvc
            .perform(get("/api/v1/learning-records?studentId=$studentId").session(studentSession))
            .andExpect(status().isForbidden)

        mockMvc
            .perform(get("/api/v1/learning-records/$recordId").session(parentSession))
            .andExpect(status().isForbidden)

        mockMvc
            .perform(get("/api/v1/learning-records?studentId=$studentId"))
            .andExpect(status().isUnauthorized)
    }

    private fun adminSession(): MockHttpSession = loginSession("test-admin", "test-admin-password")

    private fun teacherSession(adminSession: MockHttpSession): MockHttpSession =
        userSession("teacher-learning-record-${shortId()}", "teacher-password", listOf("TEACHER"), adminSession)

    private fun userSession(
        loginId: String,
        password: String,
        roles: List<String>,
        adminSession: MockHttpSession,
    ): MockHttpSession {
        createUser(loginId, password, roles, adminSession)
        return loginSession(loginId, password)
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

    private fun recordLearningRecord(
        studentId: Long,
        title: String,
        content: String,
        session: MockHttpSession,
    ): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/learning-records")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId, "title" to title, "content" to content))),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.no").doesNotExist())
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
