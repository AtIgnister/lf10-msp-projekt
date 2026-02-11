package org.lf10.stimmungsumfrage.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/")
public class FeedbackFormController {
    @GetMapping
    public ModelAndView getForm() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("FeedbackForm");
        mv.getModel().put("data", "Welcome");

        return mv;
    }

    @PostMapping("/")
    public String handleForm(
        @RequestParam String feedback,
        @RequestParam String mood, Model model
    ) {
        return "redirect:/";
    }
}