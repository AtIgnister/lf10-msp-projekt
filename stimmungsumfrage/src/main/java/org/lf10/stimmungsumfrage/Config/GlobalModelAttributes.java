package org.lf10.stimmungsumfrage.Config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserRepository userRepository;

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute
    public void addGlobalAttributes(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);

        userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
            model.addAttribute("currentUserDisplay",
                    user.getFirstname() + " " + user.getLastname());
            model.addAttribute("currentUserRole",
                    isAdmin ? "Vorgesetzter" : "Mitarbeiter");
            String departmentName = user.getDepartment() != null
                    ? user.getDepartment().getName()
                    : "Keine Abteilung";
            model.addAttribute("currentUserDepartment",
                    departmentName);
        });
    }
}
