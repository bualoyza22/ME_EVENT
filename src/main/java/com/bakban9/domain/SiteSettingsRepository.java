package com.bakban9.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, Long> {

    @Query("select s from SiteSettings s order by s.id asc limit 1")
    Optional<SiteSettings> findSingleton();
}
