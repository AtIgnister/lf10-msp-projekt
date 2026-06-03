package org.lf10.stimmungsumfrage.Config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.User;
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
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal() == "anonymousUser") {
            return;
        }

        Long id = ((User) authentication.getPrincipal()).getId();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        model.addAttribute("isAdmin", isAdmin);

        String email = authentication.getName();

        userRepository.findById(id).ifPresent(user -> {

            model.addAttribute("currentUserDisplay",
                    user.getFirstname() + " " + user.getLastname());

            model.addAttribute("currentUserRole",
                    isAdmin ? "Vorgesetzter" : "Mitarbeiter");

            model.addAttribute("currentUserDepartment",
                    user.getDepartment() != null
                            ? user.getDepartment().getName()
                            : "Keine Abteilung");
        });
    }
}
