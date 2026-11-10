package com.bakban9.domain;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m where (:status is null or m.status = :status) order by m.createdAt desc, m.id desc")
    List<Message> findByStatusFilter(String status);

    long countByStatus(String status);

    @Query("select m from Message m order by m.createdAt desc, m.id desc")
    List<Message> findLatest(Limit limit);
}
