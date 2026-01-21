package com.example.workflow_management_system.security;

import com.example.workflow_management_system.model.User;
import com.example.workflow_management_system.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        System.out.println("Loading user by username/email: " + usernameOrEmail);
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    System.out.println("User not found: " + usernameOrEmail);
                    return new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
                });

        // if (user.getPassword() == null) {
        // System.out.println("User password is null for: " + usernameOrEmail);
        // throw new UsernameNotFoundException("User password is not set");
        // }
        System.out.println("User loaded successfully: " + user.getUsername() + ", Role: " + user.getRole());
        return UserPrincipal.create(user);
    }
}
