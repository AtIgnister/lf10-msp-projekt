package org.lf10.stimmungsumfrage.Services;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Interfaces.MoodCount;
import org.lf10.stimmungsumfrage.Models.FeedbackSubmission;
import org.lf10.stimmungsumfrage.Models.Mood;
import org.lf10.stimmungsumfrage.Repositories.FeedbackSubmissionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionChartService {
    private final FeedbackSubmissionRepository feedbackSubmissionRepository;

    public List<Map<String, Object>> getMoodCountList() {
        List<MoodCount> moodCounts = feedbackSubmissionRepository.countSubmissionsByMood();

        long total = moodCounts.stream()
                .mapToLong(MoodCount::getCount)
                .sum();

        return moodCounts.stream()
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
    }
}
