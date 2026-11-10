package com.bakban9.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Products shown on the public site: visible and not deleted. {@code brandPattern} is a
     * LIKE pattern with '\' as escape character (see CatalogService), or '%' for all.
     */
    @Query("select p from Product p where p.deletedAt is null and p.visible = true "
            + "and lower(p.brand) like :brandPattern escape '\\' order by p.sortOrder asc, p.id asc")
    List<Product> findPublic(String brandPattern);

    @Query("select p from Product p where p.deletedAt is null order by p.sortOrder asc, p.id asc")
    List<Product> findActive();

    @Query("select p from Product p where p.id = :id and p.deletedAt is null")
    Optional<Product> findActiveById(Long id);

    @Query("select count(p) from Product p where p.deletedAt is null and p.visible = true")
    long countVisible();

    @Query("select coalesce(max(p.sortOrder), 0) from Product p where p.deletedAt is null")
    int maxSortOrder();

    @Query("select p from Product p where p.deletedAt is not null and p.deletedAt < :before")
    List<Product> findDeletedBefore(Instant before);

    @Query("select pi.product from ProductImage pi where pi.media.id = :mediaId and pi.product.deletedAt is null")
    List<Product> findUsingMedia(Long mediaId);
}
