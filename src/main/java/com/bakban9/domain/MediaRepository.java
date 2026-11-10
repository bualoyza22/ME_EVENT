package com.bakban9.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaRepository extends JpaRepository<Media, Long> {

    @Query("select m from Media m where m.deletedAt is null order by m.createdAt desc, m.id desc")
    List<Media> findActive();

    @Query("select m from Media m where m.deletedAt is null and m.kind = :kind order by m.createdAt desc, m.id desc")
    List<Media> findActiveByKind(String kind);

    @Query("select m from Media m where m.id = :id and m.deletedAt is null")
    Optional<Media> findActiveById(Long id);

    @Query("select m from Media m where m.id in :ids and m.deletedAt is null")
    List<Media> findActiveByIds(List<Long> ids);

    @Query("select m from Media m where m.deletedAt is not null and m.deletedAt < :before")
    List<Media> findDeletedBefore(Instant before);

    @Query(value = "select count(*) from product_images pi join products p on p.id = pi.product_id "
            + "where pi.media_id = :mediaId", nativeQuery = true)
    long countProductUsage(Long mediaId);

    @Query(value = "select count(*) from page_sections where :mediaId = any(media_ids)", nativeQuery = true)
    long countSectionUsage(Long mediaId);

    @Query(value = "select count(*) from site_settings where logo_media_id = :mediaId", nativeQuery = true)
    long countLogoUsage(Long mediaId);
}
