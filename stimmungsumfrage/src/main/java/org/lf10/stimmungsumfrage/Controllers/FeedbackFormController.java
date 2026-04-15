package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.*;
import org.lf10.stimmungsumfrage.Models.Forms.FeedbackForm;
import org.lf10.stimmungsumfrage.Repositories.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

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
    @GetMapping("feedbacks/id/{direction}")
    public String getAllSortedByID(Model model, @PathVariable String direction) {

        return sortFeedbackList("id", direction, model);
    }
    @GetMapping("feedbacks/department/{direction}")
    public String getAllSortedByAbteilung(Model model, @PathVariable String direction) {

        return sortFeedbackList("department", direction, model);
    }
    @GetMapping("feedbacks/mood/{direction}")
    public String getAllSortedByMood(Model model, @PathVariable String direction) {

        return sortFeedbackList("mood", direction, model);
    }
    @GetMapping("feedbacks/created_at/{direction}")
    public String getAllSortedByErstellung(Model model, @PathVariable String direction) {

        return sortFeedbackList("createdAt", direction, model);
    }

    private String sortFeedbackList(String propertyName, String direction, Model model)
    {
        Sort sort = direction.equalsIgnoreCase("asc")
        ? Sort.by(propertyName).ascending()
        : Sort.by(propertyName).descending();
        model.addAttribute("feedbacks", submissionRepository.findAllSorted(sort));
        return "FeedbackList";
    }
}
