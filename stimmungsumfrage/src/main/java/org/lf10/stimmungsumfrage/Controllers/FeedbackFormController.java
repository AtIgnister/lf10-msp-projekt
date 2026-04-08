package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.*;
import org.lf10.stimmungsumfrage.Models.Forms.FeedbackForm;
import org.lf10.stimmungsumfrage.Repositories.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class FeedbackFormController {

    private final UserRepository userRepository;
    private final MoodRepository moodRepository;
    private final FeedbackSubmissionRepository submissionRepository;

    // Display feedback form
    @GetMapping
    public String getForm(Model model) {
        model.addAttribute("feedbackForm", new FeedbackForm()); // bind empty form
        model.addAttribute("data", "Welcome");
        model.addAttribute("moods", moodRepository.findAll()); // <-- add this

        return "FeedbackForm";
    }

    // Handle form submission
    @PutMapping
    public String handleForm(
            @ModelAttribute("feedbackForm") FeedbackForm feedbackForm,
            Authentication authentication,
            Model model
    ) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Mood moodEntity = moodRepository.findByMoodName(feedbackForm.getMood().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Mood not found"));

        FeedbackSubmission submission = new FeedbackSubmission();
        submission.setFeedbackText(feedbackForm.getFeedback());
        submission.setMood(moodEntity);
        submission.setDepartment(user.getDepartment());

        submissionRepository.save(submission);

        model.addAttribute("feedback", feedbackForm.getFeedback());
        model.addAttribute("mood", moodEntity.getMoodName());
        return "FeedbackInputConfirmation";
    }

    // Zeigt alle Feedbacks, deren Text nicht leer ist.
    @GetMapping("feedbacks")
    public String getFeedbacksWithText(Model model) {
        model.addAttribute("feedbacks", submissionRepository.findAllWithNonEmptyFeedbackText());
        return "FeedbackList";
    }
}
