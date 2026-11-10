package com.bakban9.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminInviteRepository extends JpaRepository<AdminInvite, Long> {

    Optional<AdminInvite> findByTokenHash(String tokenHash);

    @Query("select i from AdminInvite i where i.acceptedAt is null and i.expiresAt > CURRENT_TIMESTAMP order by i.createdAt desc")
    List<AdminInvite> findPending();
}
