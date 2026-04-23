package org.lf10.stimmungsumfrage;

import org.junit.jupiter.api.Test;
import org.lf10.stimmungsumfrage.Config.DataInitializer;
import org.lf10.stimmungsumfrage.Config.SecurityConfig;
import org.lf10.stimmungsumfrage.Controllers.UserController;
import org.lf10.stimmungsumfrage.Helpers.MockData;
import org.lf10.stimmungsumfrage.Models.Department;
import org.lf10.stimmungsumfrage.Models.Role;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repositories.DepartmentRepository;
import org.lf10.stimmungsumfrage.Repositories.MoodRepository;
import org.lf10.stimmungsumfrage.Repositories.RoleRepository;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.lf10.stimmungsumfrage.Services.UserService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private MoodRepository moodRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private UserService userService;

    @Test
    void testIndexRendering() throws Exception {
        User mockAdmin = MockData.createMockAdmin();

        Page<User> mockPage = new PageImpl<>(
                List.of(mockAdmin),
                PageRequest.of(0, 10),
                1
        );

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(
                        get("/admin/users")
                                .with(user(mockAdmin))
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("users/index"));
    }

    @Test
    void canCreateUser() throws Exception {
        // Create admin mock
        User mockAdmin = MockData.createMockAdmin();
        mockAdmin.setId(1L);

        Department validDept = new Department();
        validDept.setId(1L);
        validDept.setName("Engineering");

        Role validRole = new Role();
        validRole.setId(1L);
        validRole.setName("USER");

        User newUser = new User()
                .setId(2L)
                .setFirstname("Jane")
                .setLastname("Doe")
                .setEmail("jane@example.com")
                .setPassword("oldpass")
                .setDepartment(validDept)
                .setRole(validRole)
                .setHasSubmittedFeedback(false);

        when(userService.registerUser(any(User.class))).thenReturn(newUser);

        Mockito.clearInvocations(userService);

        mockMvc.perform(post("/admin/users")
                        .with(user(mockAdmin))
                        .with(csrf())
                        .param("_method", "put")
                        .param("firstname", "John")
                        .param("lastname", "Doe")
                        .param("email", "john.doe@example.com")
                        .param("password", "12345678")
                        .param("department.id", "1")
                        .param("role.id", "1")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService, times(1)).registerUser(any(User.class));
    }

    @Test
    void canEditUser() throws Exception {
        User mockAdmin = MockData.createMockAdmin();
        mockAdmin.setId(1L);

        User existingUser = MockData.createMockUser();
        existingUser.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser));
        
        User updatedUser = new User()
                .setId(2L)
                .setFirstname("John")
                .setLastname("Updated")
                .setEmail("john.updated@example.com");

        when(userService.updateUser(any(User.class))).thenReturn(updatedUser);

        mockMvc.perform(patch("/admin/users/2")
                        .with(user(mockAdmin))
                        .with(csrf())
                        .param("firstname", "John")
                        .param("lastname", "Updated")
                        .param("email", "john.updated@example.com")
                        .param("department.id", "1")
                        .param("role.id", "2")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService, times(1)).updateUser(any(User.class));
        verify(userRepository, times(1)).findById(2L);
    }

    @Test
    void canDeleteUser() throws Exception {
        User mockAdmin = MockData.createMockAdmin();
        mockAdmin.setId(1L);

        User mockUser = MockData.createMockUser();
        mockUser.setId(2L);

        when(userRepository.findById(mockUser.getId()))
                .thenReturn(Optional.of(mockUser));

        doNothing().when(userRepository).delete(mockUser);

        // Confirm delete page
        mockMvc.perform(
                get("/admin/users/" + mockUser.getId() + "/confirm-delete")
                        .with(user(mockAdmin))
                        .with(csrf())
        ).andExpect(status().isOk());

        // Perform delete
        mockMvc.perform(
                        delete("/admin/users/" + mockUser.getId())
                                .with(user(mockAdmin))
                                .with(csrf())
                ).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    void testAccessDeniedForRegularUser() throws Exception {
        User mockUser = MockData.createMockUser();

        Page<User> mockPage = new PageImpl<>(
                List.of(mockUser),
                PageRequest.of(0, 10),
                1
        );

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(
                get("/admin/users")
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
