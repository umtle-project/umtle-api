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
import java.time.LocalDate
import java.util.UUID

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
    fun `teacher can manage students like admin`() {
        val adminSession = adminSession()
        createUser("teacher-student-api-test", "teacher-password", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession("teacher-student-api-test", "teacher-password")

        val createResponse =
            mockMvc
                .perform(
                    post("/api/v1/students")
                        .with(csrf())
                        .session(teacherSession)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("name" to "선생님등록"))),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("선생님등록"))
                .andReturn()
        val studentId = createResponse.readId()

        mockMvc
            .perform(get("/api/v1/students").session(teacherSession))
            .andExpect(status().isOk)

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "선생님수정"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("선생님수정"))

        mockMvc
            .perform(post("/api/v1/students/$studentId/deactivate").with(csrf()).session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("INACTIVE"))
    }

    @Test
    fun `admin can update student profile and read it back`() {
        val session = adminSession()
        val studentId = createStudent("프로필 학생 ${shortId()}", session)
        val birthDate = LocalDate.now().minusYears(12).toString()

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "phone" to "010-1234-5678",
                                "birthDate" to birthDate,
                                "school" to "움틀초",
                                "grade" to "6학년",
                                "memo" to "상담 메모",
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.phone").value("010-1234-5678"))
            .andExpect(jsonPath("$.birthDate").value(birthDate))
            .andExpect(jsonPath("$.school").value("움틀초"))
            .andExpect(jsonPath("$.grade").value("6학년"))
            .andExpect(jsonPath("$.memo").value("상담 메모"))

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "phone" to null,
                                "birthDate" to null,
                                "school" to null,
                                "grade" to null,
                                "memo" to "메모만 유지",
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.phone").doesNotExist())
            .andExpect(jsonPath("$.birthDate").doesNotExist())
            .andExpect(jsonPath("$.school").doesNotExist())
            .andExpect(jsonPath("$.grade").doesNotExist())
            .andExpect(jsonPath("$.memo").value("메모만 유지"))

        mockMvc
            .perform(get("/api/v1/students/$studentId").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.memo").value("메모만 유지"))
    }

    @Test
    fun `invalid student profile returns 400`() {
        val session = adminSession()
        val studentId = createStudent("프로필 검증 학생 ${shortId()}", session)

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(profilePayload(phone = "1".repeat(21))),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(profilePayload(birthDate = LocalDate.now().plusDays(1).toString())),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(profilePayload(school = "가".repeat(101))),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(profilePayload(grade = "가".repeat(21))),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(profilePayload(memo = "가".repeat(1001))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `student profile request missing a field returns 400 and keeps previous profile`() {
        val session = adminSession()
        val studentId = createStudent("프로필 누락 학생 ${shortId()}", session)
        val birthDate = LocalDate.now().minusYears(11).toString()

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(
                        profilePayload(
                            phone = "010-9999-8888",
                            birthDate = birthDate,
                            school = "움틀중",
                            grade = "1학년",
                            memo = "기존 메모",
                        ),
                    ),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "birthDate" to null,
                                "school" to "부분 학교",
                                "grade" to "2학년",
                                "memo" to "부분 메모",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(get("/api/v1/students/$studentId").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.phone").value("010-9999-8888"))
            .andExpect(jsonPath("$.birthDate").value(birthDate))
            .andExpect(jsonPath("$.school").value("움틀중"))
            .andExpect(jsonPath("$.grade").value("1학년"))
            .andExpect(jsonPath("$.memo").value("기존 메모"))
    }

    @Test
    fun `teacher can update profile and missing profile or detail student returns 404`() {
        val adminSession = adminSession()
        val teacherLoginId = "teacher-student-profile-${shortId()}"
        createUser(teacherLoginId, "teacher-password", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")
        val studentId = createStudent("권한 프로필 학생 ${shortId()}", adminSession)

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId/profile")
                    .with(csrf())
                    .session(teacherSession)
                    .contentType("application/json")
                    .content(profilePayload(phone = "010")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.phone").value("010"))

        mockMvc
            .perform(
                patch("/api/v1/students/999999999/profile")
                    .with(csrf())
                    .session(adminSession)
                    .contentType("application/json")
                    .content(profilePayload(phone = "010")),
            ).andExpect(status().isNotFound)

        mockMvc
            .perform(get("/api/v1/students/999999999/detail").session(adminSession))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `student detail returns classroom attendance and homework summaries`() {
        val adminSession = adminSession()
        val teacherLoginId = "teacher-student-detail-${shortId()}"
        createUser(teacherLoginId, "teacher-password", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")
        val classId = createClass("상세 반 ${shortId()}", adminSession)
        val studentId = createStudent("상세 학생 ${shortId()}", adminSession)
        assignStudent(classId, studentId, adminSession)

        listOf("PRESENT", "LATE", "ABSENT", "EXCUSED", "PRESENT", "ABSENT").forEach { attendanceStatus ->
            val lessonId = createLesson(classId, adminSession)
            recordAttendance(lessonId, studentId, attendanceStatus, teacherSession)
        }

        val homeworkIds =
            listOf(
                assignHomework(studentId, null, "숙제 1", teacherSession),
                assignHomework(studentId, null, "숙제 2", teacherSession),
                assignHomework(studentId, null, "숙제 3", teacherSession),
                assignHomework(studentId, null, "숙제 4", teacherSession),
                assignHomework(studentId, null, "숙제 5", teacherSession),
                assignHomework(studentId, null, "숙제 6", teacherSession),
            )
        updateHomework(homeworkIds[1], "SUBMITTED", teacherSession)
        updateHomework(homeworkIds[2], "GRADED", teacherSession)
        updateHomework(homeworkIds[4], "SUBMITTED", teacherSession)

        mockMvc
            .perform(get("/api/v1/students/$studentId/detail").session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(studentId))
            .andExpect(jsonPath("$.classrooms[0].id").value(classId))
            .andExpect(jsonPath("$.attendanceSummary.counts.PRESENT").value(2))
            .andExpect(jsonPath("$.attendanceSummary.counts.LATE").value(1))
            .andExpect(jsonPath("$.attendanceSummary.counts.ABSENT").value(2))
            .andExpect(jsonPath("$.attendanceSummary.counts.EXCUSED").value(1))
            .andExpect(jsonPath("$.attendanceSummary.recent.length()").value(5))
            .andExpect(jsonPath("$.homeworkSummary.counts.ASSIGNED").value(3))
            .andExpect(jsonPath("$.homeworkSummary.counts.SUBMITTED").value(2))
            .andExpect(jsonPath("$.homeworkSummary.counts.GRADED").value(1))
            .andExpect(jsonPath("$.homeworkSummary.recent.length()").value(5))
    }

    @Test
    fun `student detail returns empty summaries for student without related records`() {
        val session = adminSession()
        val studentId = createStudent("빈 상세 학생 ${shortId()}", session)

        mockMvc
            .perform(get("/api/v1/students/$studentId/detail").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.classrooms.length()").value(0))
            .andExpect(jsonPath("$.attendanceSummary.counts.PRESENT").value(0))
            .andExpect(jsonPath("$.attendanceSummary.counts.LATE").value(0))
            .andExpect(jsonPath("$.attendanceSummary.counts.ABSENT").value(0))
            .andExpect(jsonPath("$.attendanceSummary.counts.EXCUSED").value(0))
            .andExpect(jsonPath("$.attendanceSummary.recent.length()").value(0))
            .andExpect(jsonPath("$.homeworkSummary.counts.ASSIGNED").value(0))
            .andExpect(jsonPath("$.homeworkSummary.counts.SUBMITTED").value(0))
            .andExpect(jsonPath("$.homeworkSummary.counts.GRADED").value(0))
            .andExpect(jsonPath("$.homeworkSummary.recent.length()").value(0))
    }

    @Test
    fun `student and unauthenticated users cannot read student detail`() {
        val adminSession = adminSession()
        val studentId = createStudent("상세 권한 학생 ${shortId()}", adminSession)
        val studentLoginId = "student-detail-${shortId()}"
        createUser(studentLoginId, "student-password", listOf("STUDENT"), adminSession)
        val studentSession = loginSession(studentLoginId, "student-password")

        mockMvc
            .perform(get("/api/v1/students/$studentId/detail").session(studentSession))
            .andExpect(status().isForbidden)

        mockMvc
            .perform(get("/api/v1/students/$studentId/detail"))
            .andExpect(status().isUnauthorized)
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
        status: String,
        session: MockHttpSession,
    ): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/lessons/$lessonId/attendances")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mapOf("studentId" to studentId, "status" to status))),
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

    private fun updateHomework(
        homeworkId: Long,
        status: String,
        session: MockHttpSession,
    ) {
        mockMvc
            .perform(
                patch("/api/v1/homeworks/$homeworkId")
                    .with(csrf())
                    .session(session)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("status" to status))),
            ).andExpect(status().isOk)
    }

    private fun profilePayload(
        phone: String? = null,
        birthDate: String? = null,
        school: String? = null,
        grade: String? = null,
        memo: String? = null,
    ): String =
        objectMapper.writeValueAsString(
            mapOf(
                "phone" to phone,
                "birthDate" to birthDate,
                "school" to school,
                "grade" to grade,
                "memo" to memo,
            ),
        )

    private fun org.springframework.test.web.servlet.MvcResult.readId(): Long =
        objectMapper
            .readTree(response.contentAsString)
            .get("id")
            .asLong()

    private fun shortId(): String = UUID.randomUUID().toString().take(8)

    private fun HttpSession?.toMockHttpSession(): MockHttpSession = this as MockHttpSession
}
