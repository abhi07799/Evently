package com.abhishek.evently.dto.response;

import com.abhishek.evently.entity.enums.TicketStatusEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketResponseDTO
{
    private Long id;

    private TicketStatusEnum ticketStatus;

    private Long ticketStockId;

    private Long purchaserUserId;

    private UUID qrCodeId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
