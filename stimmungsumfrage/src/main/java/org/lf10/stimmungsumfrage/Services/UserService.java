package org.lf10.stimmungsumfrage.Services;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(User user) {

        // Hash password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public User updateUser(User updatedUser) {

        User existingUser = userRepository.findById(updatedUser.getId())
                .orElseThrow();

        boolean emailChanged =
                !existingUser.getEmail().equals(updatedUser.getEmail());

        // Update editable fields only
        existingUser.setFirstname(updatedUser.getFirstname());
        existingUser.setLastname(updatedUser.getLastname());
        existingUser.setDepartment(updatedUser.getDepartment());
        existingUser.setRole(updatedUser.getRole());

        existingUser.setEmail(updatedUser.getEmail());

        // Only encode password if changed
        if (updatedUser.getPassword() != null &&
                !updatedUser.getPassword().isBlank()) {

            existingUser.setPassword(
                    passwordEncoder.encode(updatedUser.getPassword())
            );
        }

        userRepository.save(existingUser);

        return existingUser;
    }
}
