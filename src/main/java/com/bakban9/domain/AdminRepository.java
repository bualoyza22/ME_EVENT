package com.bakban9.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    @Query("select a from Admin a where lower(a.email) = lower(:email)")
    Optional<Admin> findByEmailIgnoreCase(String email);

    @Query("select a from Admin a order by a.role desc, a.createdAt asc")
    List<Admin> findAllOrdered();
}
