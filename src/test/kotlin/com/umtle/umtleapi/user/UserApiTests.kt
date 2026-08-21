package com.umtle.umtleapi.user

import com.umtle.umtleapi.TestcontainersConfiguration
import jakarta.persistence.EntityManagerFactory
import jakarta.servlet.http.HttpSession
import org.hamcrest.Matchers.notNullValue
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class UserApiTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Test
    fun `login with valid credentials issues a session`() {
        mockMvc
            .perform(login("test-admin", "test-admin-password"))
            .andExpect(status().isOk)
            .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", notNullValue()))
            .andExpect(jsonPath("$.loginId").value("test-admin"))
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
    }

    @Test
    fun `login with invalid credentials returns 401`() {
        mockMvc
            .perform(login("test-admin", "wrong-password"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `admin can create and get users`() {
        val session = adminSession()
        val createResponse =
            mockMvc
                .perform(
                    post("/api/v1/users")
                        .with(csrf())
                        .session(session)
                        .contentType("application/json")
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "loginId" to "teacher-api-test",
                                    "password" to "teacher-password",
                                    "name" to "테스트 선생님",
                                    "roles" to listOf("TEACHER"),
                                ),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.loginId").value("teacher-api-test"))
                .andExpect(jsonPath("$.name").value("테스트 선생님"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()

        val userId =
            objectMapper
                .readTree(createResponse.response.contentAsString)
                .get("id")
                .asLong()

        mockMvc
            .perform(get("/api/v1/users/$userId").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.loginId").value("teacher-api-test"))
            .andExpect(jsonPath("$.name").value("테스트 선생님"))
    }

    @Test
    fun `teacher signup stays pending until admin approval`() {
        val loginId = "pending-teacher-${shortId()}"

        val signupResponse =
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType("application/json")
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "loginId" to loginId,
                                    "password" to "teacher-password",
                                    "name" to "가입 선생님",
                                    "role" to "TEACHER",
                                ),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.loginId").value(loginId))
                .andExpect(jsonPath("$.name").value("가입 선생님"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()

        mockMvc
            .perform(login(loginId, "teacher-password"))
            .andExpect(status().isUnauthorized)

        val userId =
            objectMapper
                .readTree(signupResponse.response.contentAsString)
                .get("id")
                .asLong()

        val adminSession = adminSession()

        mockMvc
            .perform(get("/api/v1/users/pending?role=TEACHER").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.loginId == '$loginId')]").exists())

        mockMvc
            .perform(post("/api/v1/users/$userId/approve").with(csrf()).session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        mockMvc
            .perform(login(loginId, "teacher-password"))
            .andExpect(status().isOk)
    }

    @Test
    fun `pending users query loads authorization and response data in one statement`() {
        val adminSession = adminSession()
        val loginId = "pending-query-count-${shortId()}"

        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "loginId" to loginId,
                                "password" to "teacher-password",
                                "name" to "쿼리수 선생님",
                                "role" to "TEACHER",
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)

        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        mockMvc
            .perform(get("/api/v1/users/pending?role=TEACHER").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.loginId == '$loginId')]").exists())

        assertEquals(1L, statistics.prepareStatementCount)
    }

    @Test
    fun `student signup claims a student and active teacher approves it`() {
        val adminSession = adminSession()
        val studentId = createStudent("승인학생-${shortId()}", adminSession)
        val teacherLoginId = "approval-teacher-${shortId()}"
        createUser(teacherLoginId, "teacher-password", "승인 선생님", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")
        val loginId = "pending-student-${shortId()}"

        val signupResponse =
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType("application/json")
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "loginId" to loginId,
                                    "password" to "student-password",
                                    "name" to "가입 학생",
                                    "role" to "STUDENT",
                                    "studentId" to studentId,
                                ),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andReturn()

        val userId =
            objectMapper
                .readTree(signupResponse.response.contentAsString)
                .get("id")
                .asLong()

        mockMvc
            .perform(get("/api/v1/users/pending?role=STUDENT_PARENT").session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.loginId == '$loginId')]").exists())

        mockMvc
            .perform(post("/api/v1/users/$userId/approve").with(csrf()).session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.studentId").value(studentId))

        mockMvc
            .perform(login(loginId, "student-password"))
            .andExpect(status().isOk)
    }

    @Test
    fun `student signup without studentId creates and claims a student`() {
        val adminSession = adminSession()
        val loginId = "self-student-${shortId()}"
        val studentName = "자가등록학생-${shortId()}"

        val signupResponse =
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType("application/json")
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "loginId" to loginId,
                                    "password" to "student-password",
                                    "name" to studentName,
                                    "role" to "STUDENT",
                                ),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.studentId").exists())
                .andReturn()

        val signupJson = objectMapper.readTree(signupResponse.response.contentAsString)
        val userId = signupJson.get("id").asLong()
        val studentId = signupJson.get("studentId").asLong()

        mockMvc
            .perform(get("/api/v1/students/$studentId").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(studentName))
            .andExpect(jsonPath("$.status").value("PENDING"))

        mockMvc
            .perform(post("/api/v1/users/$userId/approve").with(csrf()).session(adminSession))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/students/$studentId").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(studentName))
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        mockMvc
            .perform(get("/api/v1/students/search?name=$studentName"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == '$studentId')].name").value(studentName))
    }

    @Test
    fun `student and parent approval uses pending user id not claimed student id`() {
        val adminSession = adminSession()
        val teacherLoginId = "approval-id-teacher-${shortId()}"
        createUser(teacherLoginId, "teacher-password", "승인ID 선생님", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")
        val studentId = createStudent("승인ID학생-${shortId()}", adminSession)
        val childStudentId = createStudent("승인ID자녀-${shortId()}", adminSession)
        val pendingStudentLoginId = "approval-id-student-${shortId()}"
        val pendingParentLoginId = "approval-id-parent-${shortId()}"

        val pendingStudentUserId =
            signup(
                mapOf(
                    "loginId" to pendingStudentLoginId,
                    "password" to "student-password",
                    "name" to "대기 학생",
                    "role" to "STUDENT",
                    "studentId" to studentId,
                ),
            )
        val pendingParentUserId =
            signup(
                mapOf(
                    "loginId" to pendingParentLoginId,
                    "password" to "parent-password",
                    "name" to "대기 학부모",
                    "role" to "PARENT",
                    "studentId" to childStudentId,
                ),
            )

        mockMvc
            .perform(get("/api/v1/users/pending?role=STUDENT_PARENT").session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.loginId == '$pendingStudentLoginId')].id").value(pendingStudentUserId.toString()))
            .andExpect(jsonPath("$[?(@.loginId == '$pendingStudentLoginId')].studentId").value(studentId.toString()))
            .andExpect(jsonPath("$[?(@.loginId == '$pendingParentLoginId')].id").value(pendingParentUserId.toString()))
            .andExpect(jsonPath("$[?(@.loginId == '$pendingParentLoginId')].childStudentIds[0]").value(childStudentId.toString()))

        mockMvc
            .perform(post("/api/v1/users/$pendingStudentUserId/approve").with(csrf()).session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(pendingStudentUserId.toString()))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.studentId").value(studentId.toString()))

        mockMvc
            .perform(post("/api/v1/users/$pendingParentUserId/approve").with(csrf()).session(teacherSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(pendingParentUserId.toString()))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.childStudentIds[0]").value(childStudentId.toString()))
    }

    @Test
    fun `teacher cannot approve teacher signup and admin can approve student signup`() {
        val adminSession = adminSession()
        val teacherLoginId = "boundary-teacher-${shortId()}"
        createUser(teacherLoginId, "teacher-password", "경계 선생님", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")

        val pendingTeacherId =
            signup(
                mapOf(
                    "loginId" to "pending-teacher-boundary-${shortId()}",
                    "password" to "teacher-password",
                    "name" to "대기 선생님",
                    "role" to "TEACHER",
                ),
            )

        mockMvc
            .perform(post("/api/v1/users/$pendingTeacherId/approve").with(csrf()).session(teacherSession))
            .andExpect(status().isForbidden)

        val studentId = createStudent("권한학생-${shortId()}", adminSession)
        val pendingStudentId =
            signup(
                mapOf(
                    "loginId" to "pending-student-boundary-${shortId()}",
                    "password" to "student-password",
                    "name" to "대기 학생",
                    "role" to "STUDENT",
                    "studentId" to studentId,
                ),
            )

        mockMvc
            .perform(get("/api/v1/users/pending?role=STUDENT_PARENT").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == '$pendingStudentId')].studentId").value(studentId.toString()))

        mockMvc
            .perform(post("/api/v1/users/$pendingStudentId/approve").with(csrf()).session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.studentId").value(studentId.toString()))
    }

    @Test
    fun `rejecting student signup releases the student claim`() {
        val adminSession = adminSession()
        val studentId = createStudent("재가입학생-${shortId()}", adminSession)
        val teacherLoginId = "release-teacher-${shortId()}"
        createUser(teacherLoginId, "teacher-password", "재가입 선생님", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")
        val pendingStudentId =
            signup(
                mapOf(
                    "loginId" to "student-release-a-${shortId()}",
                    "password" to "student-password",
                    "name" to "학생A",
                    "role" to "STUDENT",
                    "studentId" to studentId,
                ),
            )

        mockMvc
            .perform(post("/api/v1/users/$pendingStudentId/reject").with(csrf()).session(teacherSession))
            .andExpect(status().isNoContent)

        signupStudent("student-release-b-${shortId()}", studentId)
    }

    @Test
    fun `rejecting self registered student signup leaves the student record`() {
        val adminSession = adminSession()
        val teacherLoginId = "self-reject-teacher-${shortId()}"
        createUser(teacherLoginId, "teacher-password", "자가거절 선생님", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")
        val loginId = "self-reject-student-${shortId()}"
        val studentName = "자가거절학생-${shortId()}"

        val signupResponse =
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType("application/json")
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "loginId" to loginId,
                                    "password" to "student-password",
                                    "name" to studentName,
                                    "role" to "STUDENT",
                                ),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andReturn()

        val responseJson = objectMapper.readTree(signupResponse.response.contentAsString)
        val userId = responseJson.get("id").asLong()
        val studentId = responseJson.get("studentId").asLong()

        mockMvc
            .perform(post("/api/v1/users/$userId/reject").with(csrf()).session(teacherSession))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(login(loginId, "student-password"))
            .andExpect(status().isUnauthorized)

        mockMvc
            .perform(get("/api/v1/students/$studentId").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(studentName))
            .andExpect(jsonPath("$.status").value("INACTIVE"))
    }

    @Test
    fun `parent signup stores claimed child and can be rejected by active teacher`() {
        val adminSession = adminSession()
        val studentId = createStudent("거절학생-${shortId()}", adminSession)
        val teacherLoginId = "reject-teacher-${shortId()}"
        createUser(teacherLoginId, "teacher-password", "거절 선생님", listOf("TEACHER"), adminSession)
        val teacherSession = loginSession(teacherLoginId, "teacher-password")
        val loginId = "pending-parent-${shortId()}"

        val signupResponse =
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType("application/json")
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "loginId" to loginId,
                                    "password" to "parent-password",
                                    "name" to "가입 학부모",
                                    "role" to "PARENT",
                                    "studentId" to studentId,
                                ),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.childStudentIds[0]").value(studentId))
                .andReturn()

        val userId =
            objectMapper
                .readTree(signupResponse.response.contentAsString)
                .get("id")
                .asLong()

        mockMvc
            .perform(post("/api/v1/users/$userId/reject").with(csrf()).session(teacherSession))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(login(loginId, "parent-password"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `signup rejects invalid roles and duplicate student claims`() {
        val adminSession = adminSession()
        val studentId = createStudent("중복학생-${shortId()}", adminSession)

        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "loginId" to "admin-signup-${shortId()}",
                                "password" to "admin-password",
                                "name" to "자가 관리자",
                                "role" to "ADMIN",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "loginId" to "student-long-name-${shortId()}",
                                "password" to "student-password",
                                "name" to "가".repeat(101),
                                "role" to "STUDENT",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)

        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "loginId" to "student-without-claim-${shortId()}",
                                "password" to "student-password",
                                "name" to "학부모",
                                "role" to "PARENT",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)

        signupStudent("student-claim-a-${shortId()}", studentId)
        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "loginId" to "student-claim-b-${shortId()}",
                                "password" to "student-password",
                                "name" to "학생B",
                                "role" to "STUDENT",
                                "studentId" to studentId,
                            ),
                        ),
                    ),
            ).andExpect(status().isConflict)
    }

    @Test
    fun `student search is public and returns only id and name`() {
        val adminSession = adminSession()
        createStudent("검색학생-${shortId()}", adminSession)

        mockMvc
            .perform(get("/api/v1/students/search?name=검색학생"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].status").doesNotExist())
    }

    @Test
    fun `me returns the current user when authenticated`() {
        val session = adminSession()

        mockMvc
            .perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.loginId").value("test-admin"))
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
    }

    @Test
    fun `me returns 401 when unauthenticated`() {
        mockMvc
            .perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `preflight request from the local frontend origin is allowed`() {
        mockMvc
            .perform(
                options("/api/v1/students")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "GET"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
    }

    @Test
    fun `logout invalidates the session`() {
        val session = adminSession()

        mockMvc
            .perform(post("/api/v1/auth/logout").with(csrf()).session(session))
            .andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/users/1").session(session))
            .andExpect(status().isUnauthorized)
    }

    private fun adminSession(): MockHttpSession =
        login("test-admin", "test-admin-password")
            .let {
                mockMvc
                    .perform(it)
                    .andExpect(status().isOk)
                    .andReturn()
                    .request.session
                    .toMockHttpSession()
            }

    private fun loginSession(
        loginId: String,
        password: String,
    ): MockHttpSession =
        mockMvc
            .perform(login(loginId, password))
            .andExpect(status().isOk)
            .andReturn()
            .request
            .session
            .toMockHttpSession()

    private fun createUser(
        loginId: String,
        password: String,
        name: String,
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
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf("loginId" to loginId, "password" to password, "name" to name, "roles" to roles),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andReturn()
        return objectMapper.readTree(response.response.contentAsString).get("id").asLong()
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
        return objectMapper.readTree(response.response.contentAsString).get("id").asLong()
    }

    private fun signupStudent(
        loginId: String,
        studentId: Long,
    ) {
        mockMvc
            .perform(
                post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "loginId" to loginId,
                                "password" to "student-password",
                                "name" to "학생A",
                                "role" to "STUDENT",
                                "studentId" to studentId,
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
    }

    private fun signup(payload: Map<String, Any>): Long {
        val response =
            mockMvc
                .perform(
                    post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)),
                ).andExpect(status().isCreated)
                .andReturn()
        return objectMapper.readTree(response.response.contentAsString).get("id").asLong()
    }

    private fun login(
        loginId: String,
        password: String,
    ) = post("/api/v1/auth/login")
        .with(csrf())
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(mapOf("loginId" to loginId, "password" to password)))

    private fun HttpSession?.toMockHttpSession(): MockHttpSession = this as MockHttpSession

    private fun shortId(): String =
        java.util.UUID
            .randomUUID()
            .toString()
            .take(8)
}
