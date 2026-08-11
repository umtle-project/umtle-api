package com.umtle.umtleapi.attendance

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
class AttendanceApiTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `teacher can record list get and update attendance while admin can read`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val classId = createClass("출결 반 ${shortId()}", adminSession)
        val studentId = createStudent("출결 학생 ${shortId()}", adminSession)
        assignStudent(classId, studentId, adminSession)
        val lessonId = createLesson(classId, adminSession)
        val attendanceId = recordAttendance(lessonId, studentId, "PRESENT", teacherSession)

        mockMvc
            .perform(get("/api/v1/lessons/$lessonId/attendances").session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(attendanceId))
            .andExpect(jsonPath("$[0].lessonId").value(lessonId))
            .andExpect(jsonPath("$[0].studentId").value(studentId))
            .andExpect(jsonPath("$[0].status").value("PRESENT"))

        mockMvc
            .perform(get("/api/v1/attendances/$attendanceId").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PRESENT"))

        mockMvc
            .perform(get("/api/v1/attendances").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").exists())

        mockMvc
            .perform(
                patch("/api/v1/attendances/$attendanceId")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("status" to "LATE"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("LATE"))
    }

    @Test
    fun `recording attendance rejects unassigned student and duplicate lesson student pair`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val classId = createClass("출결 예외 반 ${shortId()}", adminSession)
        val assignedStudentId = createStudent("배정 학생 ${shortId()}", adminSession)
        val unassignedStudentId = createStudent("미배정 학생 ${shortId()}", adminSession)
        assignStudent(classId, assignedStudentId, adminSession)
        val lessonId = createLesson(classId, adminSession)

        mockMvc
            .perform(
                post("/api/v1/lessons/$lessonId/attendances")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to unassignedStudentId, "status" to "ABSENT"))),
            ).andExpect(status().isBadRequest)

        recordAttendance(lessonId, assignedStudentId, "PRESENT", teacherSession)

        mockMvc
            .perform(
                post("/api/v1/lessons/$lessonId/attendances")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to assignedStudentId, "status" to "LATE"))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `missing lesson student and attendance ids return 404`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val classId = createClass("출결 404 반 ${shortId()}", adminSession)
        val lessonId = createLesson(classId, adminSession)

        mockMvc
            .perform(
                post("/api/v1/lessons/999999999/attendances")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to 999999999L, "status" to "PRESENT"))),
            ).andExpect(status().isNotFound)

        mockMvc
            .perform(
                post("/api/v1/lessons/$lessonId/attendances")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to 999999999L, "status" to "PRESENT"))),
            ).andExpect(status().isNotFound)

        mockMvc
            .perform(get("/api/v1/attendances/999999999").session(adminSession))
            .andExpect(status().isNotFound)

        mockMvc
            .perform(get("/api/v1/lessons/999999999/attendances").session(adminSession))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `teacher role is required to record and update attendance`() {
        val adminSession = adminSession()
        val teacherSession = teacherSession(adminSession)
        val classId = createClass("출결 권한 반 ${shortId()}", adminSession)
        val studentId = createStudent("출결 권한 학생 ${shortId()}", adminSession)
        assignStudent(classId, studentId, adminSession)
        val lessonId = createLesson(classId, adminSession)
        val attendanceId = recordAttendance(lessonId, studentId, "PRESENT", teacherSession)

        mockMvc
            .perform(
                post("/api/v1/lessons/$lessonId/attendances")
                    .with(csrf())
                    .session(adminSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId, "status" to "ABSENT"))),
            ).andExpect(status().isForbidden)

        mockMvc
            .perform(
                patch("/api/v1/attendances/$attendanceId")
                    .with(csrf())
                    .session(adminSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("status" to "EXCUSED"))),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `admin and teacher role are required to read attendance`() {
        val adminSession = adminSession()
        val studentLoginId = "student-attendance-${shortId()}"
        createUser(studentLoginId, "student-password", listOf("STUDENT"), adminSession)
        val studentSession = loginSession(studentLoginId, "student-password")

        mockMvc
            .perform(get("/api/v1/attendances").session(studentSession))
            .andExpect(status().isForbidden)

        mockMvc
            .perform(get("/api/v1/lessons/999999999/attendances").session(studentSession))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `unauthenticated access to attendance returns 401`() {
        mockMvc
            .perform(get("/api/v1/attendances"))
            .andExpect(status().isUnauthorized)
    }

    private fun adminSession(): MockHttpSession = loginSession("test-admin", "test-admin-password")

    private fun teacherSession(adminSession: MockHttpSession): MockHttpSession {
        val loginId = "teacher-attendance-${shortId()}"
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

    private fun recordAttendance(
        lessonId: Long,
        studentId: Long,
        statusValue: String,
        session: MockHttpSession,
    ): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/lessons/$lessonId/attendances")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId, "status" to statusValue))),
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
