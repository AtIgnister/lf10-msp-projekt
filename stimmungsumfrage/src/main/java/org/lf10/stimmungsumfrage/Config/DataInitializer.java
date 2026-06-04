package org.lf10.stimmungsumfrage.Config;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.*;
import org.lf10.stimmungsumfrage.Repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final FeedbackTypeRepository feedbackTypeRepository;
    private final MoodRepository moodRepository;
    private final LocationRepository locationRepository;
    private final CountryRepository countryRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            FeedbackType defaultType = feedbackTypeRepository.findByName("General")
                    .orElseGet(() -> {
                        FeedbackType type = new FeedbackType();
                        type.setName("General");
                        return feedbackTypeRepository.save(type);
                    });

            // Default moods
            String[] defaultMoods = {"HAPPY", "NEUTRAL", "SAD"};

            for (String moodName : defaultMoods) {
                moodRepository.findByMoodName(moodName)
                        .orElseGet(() -> moodRepository.save(new Mood(moodName)));
            }

            Country country = countryRepository.findByName("Germany")
                    .orElseGet(() -> countryRepository.save(new Country("Germany")));

            Location location = locationRepository.findByName("HQ")
                    .orElseGet(() -> {
                        Location loc = new Location();
                        loc.setName("HQ");
                        loc.setAddress("Main Street 1");
                        loc.setCountry(country);
                        return locationRepository.save(loc);
                    });

            Department department = departmentRepository.findByName("IT")
                    .orElseGet(() -> {
                        Department dep = new Department();
                        dep.setName("IT");
                        dep.setLocation(location); // Required
                        return departmentRepository.save(dep);
                    });

            Role roleUser = roleRepository.findByName("USER")
                    .orElseGet(() -> roleRepository.save(new Role("USER")));

            Role roleAdmin = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ADMIN")));

            if (userRepository.findByEmail("admin@test.com").isEmpty()) {
                User user = new User();
                user.setFirstname("Admin");
                user.setLastname("User");
                user.setEmail("admin@test.com");
                user.setPassword(passwordEncoder.encode("admin123"));
                user.setRole(roleAdmin);
                user.setDepartment(department);

                userRepository.save(user);

                System.out.println("Test admin user created: admin@test.com / admin123");
            }
        };
    }
}