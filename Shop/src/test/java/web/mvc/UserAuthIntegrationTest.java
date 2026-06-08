package web.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerLoginLogout() throws Exception {
        mockMvc.perform(post("/register")
                        .param("userId", "testuser")
                        .param("password", "password1234")
                        .param("userName", "Test User"))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "testuser")
                        .param("password", "password1234"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andReturn();

        String authorization = loginResult.getResponse().getHeader("Authorization");
        assertThat(authorization).startsWith("Bearer ");

        mockMvc.perform(post("/logout")
                        .header("Authorization", authorization))
                .andExpect(status().isOk());
    }
}
