package com.bakban9.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /** Includes soft-deleted rows: the email column is unique across all rows. */
    @Query("select m from Member m where lower(m.email) = lower(:email)")
    Optional<Member> findAnyByEmail(String email);

    @Query("select m from Member m where m.id = :id and m.deletedAt is null")
    Optional<Member> findActiveById(Long id);

    @Query("select m from Member m where m.deletedAt is null "
            + "and (:q = '' or lower(m.name) like :pattern escape '\\' or lower(m.email) like :pattern escape '\\' "
            + "or m.phone like :pattern escape '\\') "
            + "and (:consent is null or m.consentMarketing = :consent) "
            + "order by m.createdAt desc, m.id desc")
    List<Member> search(String q, String pattern, Boolean consent);

    @Query("select count(m) from Member m where m.deletedAt is null")
    long countActive();

    @Query("select m from Member m where m.deletedAt is not null and m.deletedAt < :before")
    List<Member> findDeletedBefore(Instant before);
}
