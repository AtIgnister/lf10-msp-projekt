package org.lf10.stimmungsumfrage.Controllers;

import org.lf10.stimmungsumfrage.Models.Forms.FeedbackForm;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repositories.DepartmentRepository;
import org.lf10.stimmungsumfrage.Repositories.RoleRepository;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.lf10.stimmungsumfrage.Security.AdminController;
import org.lf10.stimmungsumfrage.Security.AdminView;
import org.lf10.stimmungsumfrage.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@AdminController
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
    public String createUser(@ModelAttribute User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "users/create"; // Return form with errors
        }
        userRepository.save(user); // Hash password first in service
        return "redirect:/admin/users";
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
    public String deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).get();
        userRepository.delete(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).get();
        model.addAttribute("user", user);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("roles", roleRepository.findAll());
        return "users/edit";
    }

    @PostMapping("/{id}")
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

        // Update logic here using userToEdit
        userService.updateUser(userToEdit);  // Pass the specific user
        return "redirect:/admin/users";
    }

}
