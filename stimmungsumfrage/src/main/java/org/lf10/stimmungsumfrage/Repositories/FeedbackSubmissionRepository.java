package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.FeedbackSubmission;
import org.lf10.stimmungsumfrage.Models.Department;
import org.lf10.stimmungsumfrage.Models.Mood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, Long> {

    // Find all submissions for a specific mood
    List<FeedbackSubmission> findByMood(Mood mood);

    // Find all submissions for a specific department
    List<FeedbackSubmission> findByDepartment(Department department);

    // Find submissions within a date range
    List<FeedbackSubmission> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}