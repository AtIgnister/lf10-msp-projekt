package org.lf10.stimmungsumfrage.Models;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "moods")
@Data
public class Mood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mood_id")
    private Long moodId;

    @Column(name = "mood_name", nullable = false)
    private String moodName;

    public Mood() {
    }

    public Mood(String moodName) {
        this.moodName = moodName;
    }
}
