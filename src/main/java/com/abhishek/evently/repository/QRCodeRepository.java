package com.abhishek.evently.repository;

import com.abhishek.evently.entity.QRCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QRCodeRepository extends JpaRepository<QRCodeEntity, UUID>
{
    @Query(value = """
        SELECT q.*
        FROM qr_codes_tbl q
        JOIN tickets_tbl t ON q.ticket_id = t.id
        WHERE t.id = :ticketId
        AND t.purchaser_user_id = :userId
        """, nativeQuery = true)
    Optional<QRCodeEntity> findByTicketIdAndPurchaserUserId(@Param("ticketId") Long ticketId, @Param("userId") Long userId);
}
