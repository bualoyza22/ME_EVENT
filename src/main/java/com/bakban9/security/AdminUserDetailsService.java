package com.bakban9.security;

import com.bakban9.domain.AdminRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository admins;

    public AdminUserDetailsService(AdminRepository admins) {
        this.admins = admins;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return admins.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .map(AdminPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("not found"));
    }
}
