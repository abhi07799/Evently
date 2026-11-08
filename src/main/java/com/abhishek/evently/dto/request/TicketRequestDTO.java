package com.abhishek.evently.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketRequestDTO
{
    @NotBlank(message = "Ticket status is required")
    @Pattern(
            regexp = "PURCHASED|CANCELLED",
            message = "Invalid ticket status"
    )
    private String ticketStatus;

    @NotNull(message = "Ticket stock ID is required")
    @Positive(message = "Ticket stock ID must be positive")
    private Long ticketStockId;

    @NotNull(message = "Purchaser user ID is required")
    @Positive(message = "Purchaser user ID must be positive")
    private Long purchaserUserId;
}