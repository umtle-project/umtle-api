package com.umtle.umtleapi.user

import com.umtle.umtleapi.TestcontainersConfiguration
import jakarta.servlet.http.HttpSession
import org.hamcrest.Matchers.notNullValue
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
                                    "roles" to listOf("TEACHER"),
                                ),
                            ),
                        ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.loginId").value("teacher-api-test"))
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

    private fun login(
        loginId: String,
        password: String,
    ) = post("/api/v1/auth/login")
        .with(csrf())
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(mapOf("loginId" to loginId, "password" to password)))

    private fun HttpSession?.toMockHttpSession(): MockHttpSession = this as MockHttpSession
}
