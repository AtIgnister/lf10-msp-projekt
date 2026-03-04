package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.Feedback;
import org.lf10.stimmungsumfrage.Models.FeedbackSubmission;
import org.lf10.stimmungsumfrage.Models.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // Find all feedbacks for a specific submission
    List<Feedback> findBySubmission(FeedbackSubmission submission);

    // Find all feedbacks of a specific type
    List<Feedback> findByType(FeedbackType type);
}