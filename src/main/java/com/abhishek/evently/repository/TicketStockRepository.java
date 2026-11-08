package com.abhishek.evently.repository;

import com.abhishek.evently.entity.TicketStockEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketStockRepository extends JpaRepository<TicketStockEntity, Long>
{
    List<TicketStockEntity> findByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TicketStockEntity ts WHERE ts.id = :id")
    Optional<TicketStockEntity> findByIdWithLock(@Param("id") Long id);
}
