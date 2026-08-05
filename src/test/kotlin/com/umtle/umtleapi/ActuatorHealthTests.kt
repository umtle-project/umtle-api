package com.umtle.umtleapi

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ActuatorHealthTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun healthEndpointReportsUp() {
        mockMvc
            .perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(content().json("{\"status\":\"UP\"}"))
    }
}
