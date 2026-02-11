package org.lf10.stimmungsumfrage.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
public class FeedbackFormController {

    @GetMapping
    public ModelAndView getTestData() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("FeedbackForm");
        mv.getModel().put("data", "Welcome");

        return mv;
    }
}