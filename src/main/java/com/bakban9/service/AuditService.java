package com.bakban9.service;

import com.bakban9.domain.AuditLog;
import com.bakban9.domain.AuditLogRepository;
import com.bakban9.security.AdminPrincipal;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** "บันทึกการแก้ไข": who changed what and when, for every admin action. */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String action, String entityType, Long entityId, String summary) {
        Long adminId = null;
        String email = "system";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdminPrincipal p) {
            adminId = p.getId();
            email = p.getUsername();
        }
        repo.save(new AuditLog(adminId, email, action, entityType, entityId, summary));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> latest(int n) {
        return repo.findLatest(Limit.of(n));
    }
}
