package com.tickethub.security;

import com.tickethub.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a {@link AppUserDetails} by email for Spring Security's authentication manager.
 *
 * <p>Spring Security calls {@link #loadUserByUsername} during the login flow
 * (inside {@code AuthenticationManager.authenticate()}) and after the JWT filter
 * resolves a username from a token. The method name is misleading — the "username"
 * is whatever the application uses as a unique login key; here it is the email.</p>
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
    }
}

