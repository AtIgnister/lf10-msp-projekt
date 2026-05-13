package org.lf10.stimmungsumfrage.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
@Controller
public class ErrorController implements org.springframework.boot.webmvc.error.ErrorController{
    
    @RequestMapping("/error")
    public String error( HttpServletRequest request, ModelAndView modelAndView) 
    {

        return "error";
    }
}
