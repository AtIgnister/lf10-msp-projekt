package org.lf10.stimmungsumfrage.Controllers;

import org.lf10.stimmungsumfrage.Models.EmployeeFeedback;
import org.lf10.stimmungsumfrage.repository.FeedbackRepository;
import org.lf10.stimmungsumfrage.service.CrudService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/feedback")
public class CrudContoller {

    CrudService crudService;
    FeedbackRepository repository;


    @PostMapping("/addFeedback")
    public ResponseEntity<EmployeeFeedback> addFeedback(@RequestBody EmployeeFeedback feedback) {

        EmployeeFeedback createdFeedback = crudService.create(feedback);
        return ResponseEntity.ok(createdFeedback);
        }



    }

    public void read() {

    }


    public void update() {

    }

    public void delete() {

    }
}
