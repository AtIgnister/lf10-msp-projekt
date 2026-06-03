package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.Forms.FeedbackForm;

import java.io.Console;

import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repositories.DepartmentRepository;
import org.lf10.stimmungsumfrage.Repositories.RoleRepository;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.lf10.stimmungsumfrage.Security.AdminController;
import org.lf10.stimmungsumfrage.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@AdminController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserService userService;

    @GetMapping
    public String index(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Page<User> userPage = userRepository.findAll(PageRequest.of(page, size));
        model.addAttribute("userPage", userPage);
        return "users/index";
    }

    @PutMapping
    public ModelAndView createUser(@ModelAttribute @Validated User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("roles", roleRepository.findAll());
            return new ModelAndView("users/create", HttpStatus.BAD_REQUEST);
        }
        userService.registerUser(user);
        return new ModelAndView("redirect:/admin/users");
    }


    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("roles", roleRepository.findAll());
        return "users/create";
    }

    @GetMapping("/{id}/confirm-delete")
    public String confirmDelete(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).get();
        model.addAttribute("user", user);
        return "users/confirm-delete";
    }

    @DeleteMapping("/{id}")  // Use POST for final delete (safer)
    public ModelAndView deleteUser(@PathVariable Long id) {
       User user = userRepository.findById(id).orElseThrow();
       ModelAndView mav = new ModelAndView();

    String currentUsersEmail = SecurityContextHolder.getContext().getAuthentication().getName();

    if (currentUsersEmail.equals(user.getEmail())) {
        mav.setStatus(HttpStatusCode.valueOf(403));
        mav.addObject("message", "Benutzer können sich nicht selber löschen.");
        mav.setViewName( "error");
    }
    else
    {
    userRepository.delete(user);
    mav.setViewName("redirect:/admin/users?deleted=true");
    }
      return mav;
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).get();
        model.addAttribute("user", user);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("roles", roleRepository.findAll());
        return "users/edit";
    }
    
    @PatchMapping("/{id}")
    public String updateUser(
            @PathVariable Long id,  // User being edited
            @ModelAttribute("user") User userInput,
            @ModelAttribute("feedbackForm") FeedbackForm feedbackForm,
            Model model
    ) {
        // Find the user to edit BY ID, not current user
        User userToEdit = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Copy fields from form-bound userInput
        userToEdit.setFirstname(userInput.getFirstname());
        userToEdit.setLastname(userInput.getLastname());
        userToEdit.setEmail(userInput.getEmail());
        userToEdit.setDepartment(userInput.getDepartment());
        userToEdit.setRole(userInput.getRole());
        userToEdit.setEnabled(userInput.getEnabled());

        // Update logic here using userToEdit
        userService.updateUser(userToEdit);  // Pass the specific user
        return "redirect:/admin/users";
    }

}
