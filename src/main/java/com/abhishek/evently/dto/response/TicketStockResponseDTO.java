package com.abhishek.evently.dto.response;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketStockResponseDTO
{
    private Long id;

    private String ticketName;

    private String description;

    private Double ticketPrice;

    private Integer totalTickets;

    private Long eventId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
