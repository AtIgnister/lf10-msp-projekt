package org.lf10.stimmungsumfrage.Helpers;

import org.lf10.stimmungsumfrage.Models.Department;
import org.lf10.stimmungsumfrage.Models.Role;
import org.lf10.stimmungsumfrage.Models.User;

public class MockData {

    public static User createMockUser() {
        Department department = new Department();
        department.setName("Engineering");

        Role role = new Role();
        role.setName("USER");

        User user = new User();
        user.setFirstname("John");
        user.setLastname("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("password123");
        user.setDepartment(department);
        user.setRole(role);

        return user;
    }
}