package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Interfaces.MoodCount;
import org.lf10.stimmungsumfrage.Models.Channel;
import org.lf10.stimmungsumfrage.Models.ChannelFeedback;
import org.lf10.stimmungsumfrage.Models.FeedbackSubmission;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repositories.ChannelFeedbackRepository;
import org.lf10.stimmungsumfrage.Repositories.ChannelRepository;
import org.lf10.stimmungsumfrage.Repositories.FeedbackSubmissionRepository;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.lf10.stimmungsumfrage.Security.AdminController;
import org.lf10.stimmungsumfrage.Services.ChannelService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@AdminController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final FeedbackSubmissionRepository feedbackSubmissionRepository;
    private final ChannelService channelService;
    private final ChannelRepository channelRepository;
    private final ChannelFeedbackRepository channelFeedbackRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    @GetMapping
    public String index(Authentication auth,
                        @RequestParam(required = false) Long channelId,
                        Model model) {
        User user = getAuthenticatedUser(auth);
        List<Channel> userChannels = channelService.getUserChannels(user);
        model.addAttribute("userChannels", userChannels);

        Channel selectedChannel = null;
        if (channelId != null) {
            selectedChannel = channelRepository.findById(channelId)
                    .filter(ch -> ch.getMembers().contains(user))
                    .orElse(null);
        }

        boolean channelScope = selectedChannel != null;
        model.addAttribute("statsScope", channelScope ? "channel" : "department");
        model.addAttribute("selectedChannelId", channelScope ? selectedChannel.getId() : null);
        model.addAttribute("selectedChannel", selectedChannel);

        LocalDate today = LocalDate.now();
        LocalDate ninetyDaysAgo = today.minusDays(89);
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd.MM");
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartHappy = new ArrayList<>();
        List<Long> chartNeutral = new ArrayList<>();
        List<Long> chartSad = new ArrayList<>();

        if (channelScope) {
            List<ChannelFeedback> allChannelFeedback = channelFeedbackRepository
                    .findByChannelOrderByCreatedAtDesc(selectedChannel);
            Map<String, Long> totalCounts = buildChannelMoodCounts(allChannelFeedback);
            List<Map<String, Object>> normalized = normalizeMoodCounts(totalCounts);

            model.addAttribute("moodCounts", normalized);
            model.addAttribute("totalVotes", totalCounts.values().stream().mapToLong(Long::longValue).sum());

            List<ChannelFeedback> submissionsForChart = channelFeedbackRepository
                    .findByChannelAndCreatedAtBetween(
                            selectedChannel,
                            ninetyDaysAgo.atStartOfDay(),
                            today.plusDays(1).atStartOfDay());

            Map<LocalDate, List<ChannelFeedback>> byDate = submissionsForChart.stream()
                    .filter(cf -> mapEmojiToMoodName(cf.getEmoji()) != null)
                    .collect(Collectors.groupingBy(cf -> cf.getCreatedAt().toLocalDate()));

            for (int i = 0; i < 90; i++) {
                LocalDate date = ninetyDaysAgo.plusDays(i);
                chartLabels.add(date.format(labelFmt));
                List<ChannelFeedback> daySubs = byDate.getOrDefault(date, List.of());
                Map<String, Long> dayMoods = daySubs.stream()
                        .map(cf -> mapEmojiToMoodName(cf.getEmoji()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(m -> m, Collectors.counting()));
                chartHappy.add(dayMoods.getOrDefault("HAPPY", 0L));
                chartNeutral.add(dayMoods.getOrDefault("NEUTRAL", 0L));
                chartSad.add(dayMoods.getOrDefault("SAD", 0L));
            }

            model.addAttribute("recentChannelFeedbacks",
                    allChannelFeedback.stream()
                            .filter(cf -> (cf.getComment() != null && !cf.getComment().isBlank())
                                    || (cf.getEmoji() != null && !cf.getEmoji().isBlank()))
                            .limit(5)
                            .toList());
        } else {
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
                        map.put("percentage", percentage);
                        return map;
                    })
                    .toList();

            model.addAttribute("moodCounts", normalized);
            model.addAttribute("totalVotes", total);

            List<FeedbackSubmission> submissionsForChart = feedbackSubmissionRepository
                    .findByCreatedAtBetween(ninetyDaysAgo.atStartOfDay(), today.plusDays(1).atStartOfDay());

            Map<LocalDate, List<FeedbackSubmission>> byDate = submissionsForChart.stream()
                    .collect(Collectors.groupingBy(fs -> fs.getCreatedAt().toLocalDate()));

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

            model.addAttribute("recentFeedbacks",
                    feedbackSubmissionRepository.findAllWithNonEmptyFeedbackText()
                            .stream()
                            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                            .limit(5)
                            .toList());
        }

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartHappy", chartHappy);
        model.addAttribute("chartNeutral", chartNeutral);
        model.addAttribute("chartSad", chartSad);
        return "dashboard";
    }

    private Map<String, Long> buildChannelMoodCounts(List<ChannelFeedback> feedbacks) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("HAPPY", 0L);
        counts.put("NEUTRAL", 0L);
        counts.put("SAD", 0L);
        for (ChannelFeedback feedback : feedbacks) {
            String moodName = mapEmojiToMoodName(feedback.getEmoji());
            if (moodName != null) {
                counts.put(moodName, counts.get(moodName) + 1);
            }
        }
        return counts;
    }

    private List<Map<String, Object>> normalizeMoodCounts(Map<String, Long> counts) {
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("moodName", entry.getKey());
            map.put("count", entry.getValue());
            double percentage = total == 0 ? 0 : (double) entry.getValue() / total;
            map.put("percentage", percentage);
            normalized.add(map);
        }
        return normalized;
    }

    private String mapEmojiToMoodName(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            return null;
        }
        return switch (emoji) {
            case "\uD83D\uDE04", "\uD83D\uDE42" -> "HAPPY";
            case "\uD83D\uDE10" -> "NEUTRAL";
            case "\uD83D\uDE15", "\uD83D\uDE21" -> "SAD";
            default -> null;
        };
    }
}
