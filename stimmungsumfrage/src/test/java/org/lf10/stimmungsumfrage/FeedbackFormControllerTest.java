package org.lf10.stimmungsumfrage;

import org.junit.jupiter.api.Test;
import org.lf10.stimmungsumfrage.Controllers.FeedbackFormController;
import org.lf10.stimmungsumfrage.Helpers.MockData;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = FeedbackFormController.class)
public class FeedbackFormControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private MoodRepository moodRepository;

    @MockitoBean
    private FeedbackSubmissionRepository feedbackSubmissionRepository;

    @MockitoBean
    private FeedbackTypeRepository feedbackTypeRepository;

    @MockitoBean
    private FeedbackRepository feedbackRepository;

    @Test
    void testFormRendering() throws Exception {
        User mockUser = MockData.createMockUser();

        mockMvc.perform(
                    get("/")
                            .with(user(mockUser))
                            .with(csrf()))
                .andExpect(status().isOk())     // expect HTTP 200 OK
                .andExpect(view().name("FeedbackForm")); // expect the correct view name

    }

    @Test
    void testFormSubmission() throws Exception {
        User mockUser = MockData.createMockUser();
        TestingAuthenticationToken auth = new TestingAuthenticationToken(mockUser, mockUser.getPassword(), mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(
                put("/")
                        .with(csrf())
                        .param("feedback", "This is a test feedback")
                        .param("mood", "happy")
        );
    }
}
