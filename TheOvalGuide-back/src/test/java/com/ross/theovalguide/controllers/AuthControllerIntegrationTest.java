package com.ross.theovalguide.controllers;

import com.ross.theovalguide.model.UserAccount;
import com.ross.theovalguide.repo.PasswordResetTokenRepository;
import com.ross.theovalguide.repo.SessionTokenRepository;
import com.ross.theovalguide.repo.UserRepository;
import com.ross.theovalguide.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

        private static final String INITIAL_PASSWORD = "Password1!";
        private static final String UPDATED_PASSWORD = "NewPassword2!";

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private SessionTokenRepository sessionTokenRepository;

        @Autowired
        private PasswordResetTokenRepository passwordResetTokenRepository;

        @Autowired
        private AuthService authService;

        @Test
        void registerAndLoginWithUsernameAndEmail() throws Exception {
                registerUser("student", "student@example.com", "student@osu.edu", INITIAL_PASSWORD);

                assertThat(userRepository.findByUsernameIgnoreCase("student")).isPresent();

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload("student", INITIAL_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ok").value(true))
                                .andExpect(cookie().exists("oval_session"));

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload("student@example.com", INITIAL_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ok").value(true))
                                .andExpect(cookie().exists("oval_session"));

                UserAccount user = userRepository.findByUsernameIgnoreCase("student").orElseThrow();
                assertThat(sessionTokenRepository.findAllByUser(user)).hasSize(1);
        }

        @Test
        void registerWithDuplicateUsernameFails() throws Exception {
                registerUser("student", "student@example.com", "student@osu.edu", INITIAL_PASSWORD);

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registrationPayload("student", "other@example.com", "other@osu.edu",
                                                INITIAL_PASSWORD)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Username already in use"));
        }

        @Test
        void forgotPasswordGeneratesToken() throws Exception {
                registerUser("student", "student@example.com", "student@osu.edu", INITIAL_PASSWORD);
                UserAccount user = userRepository.findByUsernameIgnoreCase("student").orElseThrow();
                assertThat(passwordResetTokenRepository.findAllByUser(user)).isEmpty();

                mockMvc.perform(post("/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(forgotPasswordPayload("student@example.com")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ok").value(true));

                assertThat(passwordResetTokenRepository.findAllByUser(user)).hasSize(1);
        }

        @Test
        void updatePasswordChangesCredentialsAndInvalidatesSessions() throws Exception {
                registerUser("student", "student@example.com", "student@osu.edu", INITIAL_PASSWORD);

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload("student", INITIAL_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ok").value(true));

                UserAccount user = userRepository.findByUsernameIgnoreCase("student").orElseThrow();
                assertThat(sessionTokenRepository.findAllByUser(user)).hasSize(1);

                Optional<String> token = authService.initiatePasswordReset("student@example.com");
                assertThat(token).isPresent();

                mockMvc.perform(post("/auth/update-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updatePasswordPayload(UPDATED_PASSWORD, token.orElseThrow())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ok").value(true));

                user = userRepository.findByUsernameIgnoreCase("student").orElseThrow();
                assertThat(sessionTokenRepository.findAllByUser(user)).isEmpty();

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload("student", INITIAL_PASSWORD)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Invalid email/username or password"));

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload("student", UPDATED_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ok").value(true));

                user = userRepository.findByUsernameIgnoreCase("student").orElseThrow();
                assertThat(sessionTokenRepository.findAllByUser(user)).hasSize(1);
        }

        private void registerUser(String username, String email, String schoolEmail, String password) throws Exception {
                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registrationPayload(username, email, schoolEmail, password)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.ok").value(true));
        }

        private String registrationPayload(String username, String email, String schoolEmail, String password) {
                return """
                                {
                                  \"username\": \"%s\",
                                  \"email\": \"%s\",
                                  \"schoolEmail\": \"%s\",
                                  \"password\": \"%s\"
                                }
                                """.formatted(username, email, schoolEmail, password);
        }

        private String loginPayload(String login, String password) {
                return """
                                {
                                  \"login\": \"%s\",
                                  \"password\": \"%s\"
                                }
                                """.formatted(login, password);
        }

        private String forgotPasswordPayload(String email) {
                return """
                                {
                                  \"email\": \"%s\"
                                }
                                """.formatted(email);
        }

        private String updatePasswordPayload(String password, String token) {
                return """
                                {
                                  \"password\": \"%s\",
                                  \"token\": \"%s\"
                                }
                                """.formatted(password, token);
        }
}
