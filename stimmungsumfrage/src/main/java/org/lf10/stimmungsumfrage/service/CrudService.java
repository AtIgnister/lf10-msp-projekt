package org.lf10.stimmungsumfrage.service;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.EmployeeFeedback;
import org.lf10.stimmungsumfrage.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CrudService {

    private final FeedbackRepository feedbackRepository;

    public EmployeeFeedback create(EmployeeFeedback feedback) {
        return feedbackRepository.save(feedback);
    }

    public EmployeeFeedback getById(String id) {
        return feedbackRepository.findById(id).orElse(null);
    }

    public List<EmployeeFeedback> getAll() {
        return feedbackRepository.findAll();
    }

    public EmployeeFeedback update(EmployeeFeedback feedback) {
        if (feedback.getId() == null || !feedbackRepository.existsById(feedback.getId().toString())) {
            throw new RuntimeException("Feedback not found with id: " + feedback.getId());
        }
        return feedbackRepository.save(feedback);
    }

    public void delete(String id) {
        feedbackRepository.deleteById(id);
    }
}
