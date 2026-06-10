package web.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import web.mvc.domain.User;
import web.mvc.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerLogin() throws Exception {
        String userId = "testuser-" + System.currentTimeMillis();

        mockMvc.perform(post("/register")
                        .param("userId", userId)
                        .param("password", "password1234")
                        .param("userName", "Test User"))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", userId)
                        .param("password", "password1234"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andReturn();

        String authorization = loginResult.getResponse().getHeader("Authorization");
        assertThat(authorization).startsWith("Bearer ");
    }

    @Test
    void createAdminAccountAndLogin() throws Exception {
        String adminId = "admin-" + System.currentTimeMillis();
        String rawPassword = "admin1234";

        User admin = User.builder()
                .userId(adminId)
                .password(passwordEncoder.encode(rawPassword))
                .userName("Admin User")
                .role("ROLE_ADMIN")
                .build();
        userRepository.save(admin);

        mockMvc.perform(post("/login")
                        .param("username", adminId)
                        .param("password", rawPassword))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(jsonPath("$.userId").value(adminId))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }
}
