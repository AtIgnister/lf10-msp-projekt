package org.lf10.stimmungsumfrage.Models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "feedback")
@Data
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_submission_id", nullable = false)
    private FeedbackSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_type_id", nullable = false)
    private FeedbackType type;
}
