package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.Mood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MoodRepository extends JpaRepository<Mood, Long> {

    Optional<Mood> findByMoodName(String moodName);
}