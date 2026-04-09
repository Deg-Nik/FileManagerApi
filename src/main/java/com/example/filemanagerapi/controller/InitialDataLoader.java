package com.example.filemanagerapi.controller;

import com.example.filemanagerapi.entity.User;
import com.example.filemanagerapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class InitialDataLoader implements CommandLineRunner {
    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User user = new User();
            user.setUsername("john_doe");
            user.setEmail("john@example.com");
            user.setPassword("password");

            userRepository.save(user);
            System.out.println("Test user created: " + user.getId() + ", email: " + user.getEmail());
        }
    }
}
