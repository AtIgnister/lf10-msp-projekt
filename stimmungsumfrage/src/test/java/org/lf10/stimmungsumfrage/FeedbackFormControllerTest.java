package org.lf10.stimmungsumfrage;

import org.junit.jupiter.api.Test;
import org.lf10.stimmungsumfrage.Controllers.FeedbackFormController;
import org.lf10.stimmungsumfrage.Helpers.MockData;
import org.lf10.stimmungsumfrage.Models.Mood;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.mockito.Mockito.when;
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
                .andExpect(status().isOk())
                .andExpect(view().name("FeedbackForm"));

    }

    @Test
    void testFormSubmission() throws Exception {
        User mockUser = MockData.createMockUser();

        Mood mood = new Mood();
        mood.setMoodName("HAPPY");

        when(moodRepository.findByMoodName("HAPPY"))
                .thenReturn(Optional.of(mood));

        when(userRepository.findByEmail(mockUser.getEmail()))
                .thenReturn(Optional.of(mockUser));

        mockMvc.perform(
                put("/")
                        .principal(new UsernamePasswordAuthenticationToken(mockUser.getEmail(), "n/a"))
                        .with(csrf())
                        .param("feedback", "This is a test feedback")
                        .param("mood", "happy")
        )
                .andExpect(status().isOk())
                .andExpect(view().name("FeedbackInputConfirmation"));
    }
}
