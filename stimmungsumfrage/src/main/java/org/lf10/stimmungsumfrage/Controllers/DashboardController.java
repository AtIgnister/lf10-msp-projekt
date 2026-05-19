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
            List<String> channelEmojis = selectedChannel.getFeedbackEmojis();
            List<String> channelLabels = selectedChannel.getFeedbackEmojiLabels();
            int n = channelEmojis.size();

            List<ChannelFeedback> allChannelFeedback = channelFeedbackRepository
                    .findByChannelOrderByCreatedAtDesc(selectedChannel);

            long[] counts = new long[n];
            long total = 0;
            for (ChannelFeedback cf : allChannelFeedback) {
                int idx = channelEmojis.indexOf(cf.getEmoji());
                if (idx >= 0) {
                    counts[idx]++;
                    total++;
                }
            }

            List<Map<String, Object>> normalized = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Map<String, Object> m = new HashMap<>();
                m.put("emoji", channelEmojis.get(i));
                m.put("moodName", channelLabels.get(i));
                m.put("count", counts[i]);
                m.put("percentage", total == 0 ? 0.0 : (double) counts[i] / total);
                m.put("colorClass", colorClassForIndex(i, n));
                normalized.add(m);
            }
            model.addAttribute("moodCounts", normalized);
            model.addAttribute("totalVotes", total);

            List<ChannelFeedback> submissionsForChart = channelFeedbackRepository
                    .findByChannelAndCreatedAtBetween(
                            selectedChannel,
                            ninetyDaysAgo.atStartOfDay(),
                            today.plusDays(1).atStartOfDay());

            Map<LocalDate, long[]> byDate = new HashMap<>();
            for (ChannelFeedback cf : submissionsForChart) {
                int idx = channelEmojis.indexOf(cf.getEmoji());
                if (idx < 0) continue;
                byDate.computeIfAbsent(cf.getCreatedAt().toLocalDate(), k -> new long[n])[idx]++;
            }

            List<List<Long>> seriesData = new ArrayList<>();
            for (int i = 0; i < n; i++) seriesData.add(new ArrayList<>());
            for (int i = 0; i < 90; i++) {
                LocalDate date = ninetyDaysAgo.plusDays(i);
                chartLabels.add(date.format(labelFmt));
                long[] day = byDate.getOrDefault(date, new long[n]);
                for (int j = 0; j < n; j++) seriesData.get(j).add(day[j]);
            }

            List<Map<String, Object>> chartSeries = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Map<String, Object> s = new HashMap<>();
                s.put("label", channelLabels.get(i));
                s.put("emoji", channelEmojis.get(i));
                s.put("color", colorForIndex(i, n));
                s.put("background", backgroundForIndex(i, n));
                s.put("data", seriesData.get(i));
                chartSeries.add(s);
            }
            model.addAttribute("chartSeries", chartSeries);

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

    private static final String[] PALETTE_BORDER = {
            "#f87171", "#fb923c", "#fbbf24", "#34d399", "#10b981"
    };
    private static final String[] PALETTE_BG = {
            "rgba(248,113,113,0.1)", "rgba(251,146,60,0.1)", "rgba(251,191,36,0.1)",
            "rgba(52,211,153,0.1)", "rgba(16,185,129,0.1)"
    };

    private int paletteIndex(int index, int total) {
        if (total <= 1) return 0;
        return (int) Math.round((double) index * (PALETTE_BORDER.length - 1) / (total - 1));
    }

    private String colorForIndex(int index, int total) {
        return PALETTE_BORDER[paletteIndex(index, total)];
    }

    private String backgroundForIndex(int index, int total) {
        return PALETTE_BG[paletteIndex(index, total)];
    }

    private String colorClassForIndex(int index, int total) {
        if (total <= 1) return "neutral";
        double ratio = (double) index / (total - 1);
        if (ratio < 0.34) return "bad";
        if (ratio < 0.67) return "neutral";
        return "good";
    }
}
