package com.example.workflow_management_system.security;

import com.example.workflow_management_system.model.User;
import com.example.workflow_management_system.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@org.springframework.transaction.annotation.Transactional
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        try {
            User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));

            // Force initialization of lazy-loaded tenant relationship within the
            // transaction
            if (user.getTenant() != null) {
                user.getTenant().getName();
            }

            return UserPrincipal.create(user);
        } catch (Exception e) {
            System.err.println("Error loading user: " + usernameOrEmail);
            e.printStackTrace();
            throw e;
        }
    }
}
