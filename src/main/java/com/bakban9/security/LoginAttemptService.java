package com.bakban9.security;

import com.bakban9.config.AppProperties;
import com.bakban9.domain.AdminRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Spec: lock the account temporarily after 5 wrong passwords. */
@Service
public class LoginAttemptService {

    private final AdminRepository admins;
    private final AppProperties props;

    public LoginAttemptService(AdminRepository admins, AppProperties props) {
        this.admins = admins;
        this.props = props;
    }

    /** @return true when this failure locked the account */
    @Transactional
    public boolean recordFailure(String email) {
        return admins.findByEmailIgnoreCase(email == null ? "" : email.trim()).map(a -> {
            int n = a.getFailedAttempts() + 1;
            if (n >= props.security().maxFailedLogins()) {
                a.setFailedAttempts(0);
                a.setLockedUntil(Instant.now().plus(Duration.ofMinutes(props.security().lockMinutes())));
                return true;
            }
            a.setFailedAttempts(n);
            return false;
        }).orElse(false);
    }

    @Transactional
    public void recordSuccess(Long adminId) {
        admins.findById(adminId).ifPresent(a -> {
            a.setFailedAttempts(0);
            a.setLockedUntil(null);
            a.setLastLoginAt(Instant.now());
        });
    }
}
