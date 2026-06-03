package org.lf10.stimmungsumfrage.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_channel_feedback_status",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "channel_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserChannelFeedbackStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @Column(name = "last_submission")
    private LocalDateTime lastSubmission;
}