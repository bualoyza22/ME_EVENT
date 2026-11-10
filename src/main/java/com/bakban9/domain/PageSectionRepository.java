package com.bakban9.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PageSectionRepository extends JpaRepository<PageSection, Long> {

    @Query("select s from PageSection s where s.page.slug = :slug order by s.sortOrder asc, s.id asc")
    List<PageSection> findByPageSlug(String slug);

    @Query("select s from PageSection s where s.page.slug = :slug and s.key = :key")
    Optional<PageSection> findByPageSlugAndKey(String slug, String key);

    @Query(value = "select * from page_sections where :mediaId = any(media_ids)", nativeQuery = true)
    List<PageSection> findUsingMedia(Long mediaId);
}
