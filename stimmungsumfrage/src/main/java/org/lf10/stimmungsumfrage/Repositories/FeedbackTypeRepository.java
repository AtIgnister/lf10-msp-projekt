package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackTypeRepository extends JpaRepository<FeedbackType, Long> {

    Optional<FeedbackType> findByName(String name);
}