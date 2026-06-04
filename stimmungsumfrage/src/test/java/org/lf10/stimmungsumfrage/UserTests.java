package org.lf10.stimmungsumfrage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lf10.stimmungsumfrage.Config.DataInitializer;
import org.lf10.stimmungsumfrage.Models.Department;
import org.lf10.stimmungsumfrage.Models.Location;
import org.lf10.stimmungsumfrage.Models.Role;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.lf10.stimmungsumfrage.Services.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserTests {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void registerUser_ShouldEncodePassword_AndSaveUser() {
        // Arrange
        Department department = new Department();
        Location location = new Location();
        department.setLocation(location);

        Role role = new Role();

        User user = new User();
        user.setPassword("123456");
        user.setFirstname("Max");
        user.setLastname("Mustermann");
        user.setRole(role);
        user.setEmail("test@test.com");
        user.setDepartment(department);

        when(passwordEncoder.encode(any()))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User savedUser = userService.registerUser(user);

        // Assert
        assertEquals("encodedPassword", savedUser.getPassword());

        verify(passwordEncoder, times(1)).encode("123456");
        verify(userRepository, times(1)).save(user);
    }
}