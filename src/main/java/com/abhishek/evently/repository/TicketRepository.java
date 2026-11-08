package com.abhishek.evently.repository;

import com.abhishek.evently.entity.TicketEntity;
import com.abhishek.evently.entity.enums.TicketStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long>
{
    Page<TicketEntity> findByTicketStockId(Long ticketStockId, Pageable pageable);

    Page<TicketEntity> findByPurchaserUserId(Long purchaserUserId, Pageable pageable);

    long countByTicketStockIdAndTicketStatus(
            Long ticketStockId, TicketStatusEnum ticketStatus);

}
