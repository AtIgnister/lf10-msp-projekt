package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Interfaces.MoodCount;
import org.lf10.stimmungsumfrage.Repositories.FeedbackSubmissionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final FeedbackSubmissionRepository feedbackSubmissionRepository;

    @GetMapping
    public String index(Model model) {
        List<MoodCount> moodCounts = feedbackSubmissionRepository.countSubmissionsByMood();

        long total = moodCounts.stream()
                .mapToLong(MoodCount::getCount)
                .sum();

        List<Map<String, Object>> normalized = moodCounts.stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("moodName", m.getMoodName());
                    map.put("count", m.getCount());

                    // IMPORTANT: avoid division by zero
                    double percentage = total == 0 ? 0 : (double) m.getCount() / total;

                    // ensure a tiny visible bar for zero values (optional)
                    if (percentage == 0) {
                        percentage = 0.01; // small visible bar
                    }

                    map.put("percentage", percentage);
                    return map;
                })
                .toList();

        model.addAttribute("moodCounts", normalized);
        return "dashboard";
    }
}
