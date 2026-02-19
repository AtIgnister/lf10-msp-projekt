package org.lf10.stimmungsumfrage.Services;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repository.UserRepository;
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
}
