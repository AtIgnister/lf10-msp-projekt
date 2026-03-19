package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Interfaces.MoodCount;
import org.lf10.stimmungsumfrage.Repositories.FeedbackSubmissionRepository;
import org.lf10.stimmungsumfrage.Services.SubmissionChartService;
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
    private final SubmissionChartService submissionChartService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("moodCounts", submissionChartService.getMoodCountList());
        return "dashboard";
    }
}
