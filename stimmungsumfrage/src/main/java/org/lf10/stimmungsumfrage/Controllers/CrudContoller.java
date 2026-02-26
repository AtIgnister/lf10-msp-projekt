package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.EmployeeFeedback;
import org.lf10.stimmungsumfrage.service.CrudService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequiredArgsConstructor
@RequestMapping("/feedback")
public class CrudContoller {

    final CrudService crudService;


    @PostMapping("/addFeedback")
    public ResponseEntity<EmployeeFeedback> addFeedback(@RequestBody EmployeeFeedback feedback) {

        EmployeeFeedback createdFeedback = crudService.create(feedback);
        return ResponseEntity.ok(createdFeedback);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeFeedback> getFeedbackById(@PathVariable String id) {
        EmployeeFeedback feedback = crudService.getById(id);
        if (feedback != null) {
            return ResponseEntity.ok(feedback);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/all")
    public ResponseEntity<List<EmployeeFeedback>> getFeedbackAll() {
        List<EmployeeFeedback> feedback = crudService.getAll();
        return ResponseEntity.ok(feedback);
    }



    @PutMapping("/updateFeedback")
    public ResponseEntity<EmployeeFeedback> updateFeedback(@RequestBody EmployeeFeedback feedback) {
        EmployeeFeedback updatedFeedback = crudService.update(feedback);
        return ResponseEntity.ok(updatedFeedback);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<EmployeeFeedback> delete(@PathVariable String id) {
        EmployeeFeedback feedback = crudService.getById(id);
        if (feedback != null) {
            crudService.delete(id);
            return ResponseEntity.ok(feedback);
        } else {
            return ResponseEntity.notFound().build();
        }

    }
}
