package org.lf10.stimmungsumfrage.Models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "feedback_type")
@Data
public class FeedbackType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
