package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Interfaces.MoodCount;
import org.lf10.stimmungsumfrage.Models.FeedbackSubmission;
import org.lf10.stimmungsumfrage.Repositories.FeedbackSubmissionRepository;
import org.lf10.stimmungsumfrage.Security.AdminController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@AdminController
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

                    double percentage = total == 0 ? 0 : (double) m.getCount() / total;
                    if (percentage == 0) {
                        percentage = 0.01;
                    }
                    map.put("percentage", percentage);
                    return map;
                })
                .toList();

        model.addAttribute("moodCounts", normalized);
        model.addAttribute("totalVotes", total);

        // Chart trend data — last 90 days
        LocalDate today = LocalDate.now();
        LocalDate ninetyDaysAgo = today.minusDays(89);
        List<FeedbackSubmission> submissionsForChart = feedbackSubmissionRepository
                .findByCreatedAtBetween(ninetyDaysAgo.atStartOfDay(), today.plusDays(1).atStartOfDay());

        Map<LocalDate, List<FeedbackSubmission>> byDate = submissionsForChart.stream()
                .collect(Collectors.groupingBy(fs -> fs.getCreatedAt().toLocalDate()));

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd.MM");
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartHappy = new ArrayList<>();
        List<Long> chartNeutral = new ArrayList<>();
        List<Long> chartSad = new ArrayList<>();

        for (int i = 0; i < 90; i++) {
            LocalDate date = ninetyDaysAgo.plusDays(i);
            chartLabels.add(date.format(labelFmt));
            List<FeedbackSubmission> daySubs = byDate.getOrDefault(date, List.of());
            Map<String, Long> dayMoods = daySubs.stream()
                    .collect(Collectors.groupingBy(fs -> fs.getMood().getMoodName(), Collectors.counting()));
            chartHappy.add(dayMoods.getOrDefault("HAPPY", 0L));
            chartNeutral.add(dayMoods.getOrDefault("NEUTRAL", 0L));
            chartSad.add(dayMoods.getOrDefault("SAD", 0L));
        }

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartHappy", chartHappy);
        model.addAttribute("chartNeutral", chartNeutral);
        model.addAttribute("chartSad", chartSad);

        model.addAttribute("recentFeedbacks",
                feedbackSubmissionRepository.findAllWithNonEmptyFeedbackText()
                        .stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .limit(5)
                        .toList());
        return "dashboard";
    }
}
