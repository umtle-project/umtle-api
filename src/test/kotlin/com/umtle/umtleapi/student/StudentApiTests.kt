package com.umtle.umtleapi.student

import com.umtle.umtleapi.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
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
        val registerResponse =
            mockMvc
                .perform(
                    post("/api/v1/students")
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
            .perform(get("/api/v1/students/$studentId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("홍길동"))

        mockMvc
            .perform(get("/api/v1/students"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").exists())

        mockMvc
            .perform(
                patch("/api/v1/students/$studentId")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "김철수"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("김철수"))

        mockMvc
            .perform(post("/api/v1/students/$studentId/deactivate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("INACTIVE"))
    }

    @Test
    fun `getting a non-existent student returns 404`() {
        mockMvc
            .perform(get("/api/v1/students/999999999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `registering with a blank name returns 400`() {
        mockMvc
            .perform(
                post("/api/v1/students")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to ""))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `registering with a name longer than the database column returns 400`() {
        mockMvc
            .perform(
                post("/api/v1/students")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "가".repeat(101)))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `updating with a name longer than the database column returns 400`() {
        val registerResponse =
            mockMvc
                .perform(
                    post("/api/v1/students")
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
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(mapOf("name" to "가".repeat(101)))),
            ).andExpect(status().isBadRequest)
    }
}
