package org.lf10.stimmungsumfrage;

import org.junit.jupiter.api.Test;
import org.lf10.stimmungsumfrage.Config.SecurityConfig;
import org.lf10.stimmungsumfrage.Controllers.UserController;
import org.lf10.stimmungsumfrage.Helpers.MockData;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Repositories.DepartmentRepository;
import org.lf10.stimmungsumfrage.Repositories.MoodRepository;
import org.lf10.stimmungsumfrage.Repositories.RoleRepository;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.lf10.stimmungsumfrage.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

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
        User mockAdmin = MockData.createMockAdmin();
        mockAdmin.setId(1L);

        User newUser = MockData.createMockUser();
        newUser.setId(2L);

        // Stub repository save
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        mockMvc.perform(
                        put("/admin/users")
                                .with(user(mockAdmin))
                                .with(csrf())
                                .param("firstname", "John")
                                .param("lastname", "Doe")
                                .param("email", "john.doe@example.com")
                        // add other required User fields
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        // Verify repository.save was called
        verify(userRepository, times(1)).save(any(User.class));
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
