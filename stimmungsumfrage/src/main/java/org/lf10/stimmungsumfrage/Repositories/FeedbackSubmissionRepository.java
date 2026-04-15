package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Interfaces.MoodCount;
import org.lf10.stimmungsumfrage.Models.FeedbackSubmission;
import org.lf10.stimmungsumfrage.Models.Department;
import org.lf10.stimmungsumfrage.Models.Mood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, Long> {

    // Find all submissions for a specific mood
    List<FeedbackSubmission> findByMood(Mood mood);

    // Find all submissions for a specific department
    List<FeedbackSubmission> findByDepartment(Department department);

    // Find submissions within a date range
    List<FeedbackSubmission> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT fs FROM FeedbackSubmission fs WHERE fs.feedbackText IS NOT NULL AND TRIM(fs.feedbackText) <> ''")
    List<FeedbackSubmission> findAllWithNonEmptyFeedbackText();

    @Query("""
    SELECT fs
    FROM FeedbackSubmission fs
    WHERE fs.feedbackText IS NOT NULL
      AND TRIM(fs.feedbackText) <> ''
      AND (:departmentId IS NULL OR fs.department.id = :departmentId)
      AND (:startDate IS NULL OR fs.createdAt >= :startDate)
      AND (:endDate IS NULL OR fs.createdAt < :endDate)
    ORDER BY fs.createdAt DESC
""")
    List<FeedbackSubmission> findAllWithNonEmptyFeedbackTextFiltered(
            @Param("departmentId") Long departmentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT m.moodName AS moodName, COUNT(f) AS count
    FROM Mood m
    LEFT JOIN FeedbackSubmission f ON f.mood = m
    GROUP BY m.moodName
""")
    List<MoodCount> countSubmissionsByMood();
}