package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.*;
import org.lf10.stimmungsumfrage.Models.Forms.FeedbackForm;
import org.lf10.stimmungsumfrage.Repositories.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class FeedbackFormController {

    private final UserRepository userRepository;
    private final MoodRepository moodRepository;
    private final FeedbackSubmissionRepository submissionRepository;
    private final DepartmentRepository departmentRepository;

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
    public String getFeedbacksWithText(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        LocalDateTime startDate = date != null ? date.atStartOfDay() : null;
        LocalDateTime endDate = date != null ? date.plusDays(1).atStartOfDay() : null;

        model.addAttribute("feedbacks",
                submissionRepository.findAllWithNonEmptyFeedbackTextFiltered(departmentId, startDate, endDate));
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedDate", date);
        return "FeedbackList";
    }
}
