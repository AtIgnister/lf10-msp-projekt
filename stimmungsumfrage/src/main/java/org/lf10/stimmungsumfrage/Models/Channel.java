package org.lf10.stimmungsumfrage.Models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "channels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Channel {

    public static final int MIN_FEEDBACK_EMOJI_COUNT = 2;
    public static final int MAX_FEEDBACK_EMOJI_COUNT = 5;

    public static final String DEFAULT_EMOJI_VERY_BAD = "😡";
    public static final String DEFAULT_EMOJI_BAD = "😕";
    public static final String DEFAULT_EMOJI_NEUTRAL = "😐";
    public static final String DEFAULT_EMOJI_GOOD = "🙂";
    public static final String DEFAULT_EMOJI_VERY_GOOD = "😄";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User creator;

    @Column(name = "feedback_scale_size")
    private Integer feedbackScaleSize;

    @Column(name = "emoji_very_bad", length = 32)
    private String emojiVeryBad;

    @Column(name = "emoji_bad", length = 32)
    private String emojiBad;

    @Column(name = "emoji_neutral", length = 32)
    private String emojiNeutral;

    @Column(name = "emoji_good", length = 32)
    private String emojiGood;

    @Column(name = "emoji_very_good", length = 32)
    private String emojiVeryGood;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "channel_members",
            joinColumns = @JoinColumn(name = "channel_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> members = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.feedbackScaleSize == null) {
            this.feedbackScaleSize = MAX_FEEDBACK_EMOJI_COUNT;
        }
    }

    @Transient
    public List<String> getFeedbackEmojis() {
        return switch (getEffectiveFeedbackScaleSize()) {
            case 2 -> List.of(
                    normalizeEmoji(emojiBad, DEFAULT_EMOJI_BAD),
                    normalizeEmoji(emojiGood, DEFAULT_EMOJI_GOOD)
            );
            case 3 -> List.of(
                    normalizeEmoji(emojiBad, DEFAULT_EMOJI_BAD),
                    normalizeEmoji(emojiNeutral, DEFAULT_EMOJI_NEUTRAL),
                    normalizeEmoji(emojiGood, DEFAULT_EMOJI_GOOD)
            );
            case 4 -> List.of(
                    normalizeEmoji(emojiVeryBad, DEFAULT_EMOJI_VERY_BAD),
                    normalizeEmoji(emojiBad, DEFAULT_EMOJI_BAD),
                    normalizeEmoji(emojiGood, DEFAULT_EMOJI_GOOD),
                    normalizeEmoji(emojiVeryGood, DEFAULT_EMOJI_VERY_GOOD)
            );
            default -> List.of(
                    normalizeEmoji(emojiVeryBad, DEFAULT_EMOJI_VERY_BAD),
                    normalizeEmoji(emojiBad, DEFAULT_EMOJI_BAD),
                    normalizeEmoji(emojiNeutral, DEFAULT_EMOJI_NEUTRAL),
                    normalizeEmoji(emojiGood, DEFAULT_EMOJI_GOOD),
                    normalizeEmoji(emojiVeryGood, DEFAULT_EMOJI_VERY_GOOD)
            );
        };
    }

    @Transient
    public List<String> getFeedbackEmojiLabels() {
        return switch (getEffectiveFeedbackScaleSize()) {
            case 2 -> List.of("Schlecht", "Gut");
            case 3 -> List.of("Schlecht", "Neutral", "Gut");
            case 4 -> List.of("Sehr schlecht", "Schlecht", "Gut", "Sehr gut");
            default -> List.of("Sehr schlecht", "Schlecht", "Neutral", "Gut", "Sehr gut");
        };
    }

    @Transient
    public int getEffectiveFeedbackScaleSize() {
        if (feedbackScaleSize == null) {
            return MAX_FEEDBACK_EMOJI_COUNT;
        }
        return Math.max(MIN_FEEDBACK_EMOJI_COUNT, Math.min(MAX_FEEDBACK_EMOJI_COUNT, feedbackScaleSize));
    }

    private String normalizeEmoji(String emoji, String defaultEmoji) {
        return emoji == null || emoji.isBlank() ? defaultEmoji : emoji;
    }
}
