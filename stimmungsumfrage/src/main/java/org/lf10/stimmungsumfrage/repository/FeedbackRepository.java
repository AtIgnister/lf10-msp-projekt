package org.lf10.stimmungsumfrage.repository;

import org.lf10.stimmungsumfrage.Models.EmployeeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<EmployeeFeedback, String> {
}
