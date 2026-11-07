package com.abhishek.evently.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class TicketStockRequestDTO
{
    @NotBlank(message = "Ticket name is required")
    @Size(min = 3, max = 100, message = "Ticket name must be between 3 and 100 characters")
    private String ticketName;

    @NotBlank(message = "Ticket description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Ticket price is required")
    @PositiveOrZero(message = "Ticket price cannot be negative")
    private Double ticketPrice;

    @NotNull(message = "Total tickets is required")
    @Positive(message = "Total tickets must be greater than zero")
    private Integer totalTickets;

    @NotNull(message = "Event ID is required")
    @Positive(message = "Event ID must be positive")
    private Long eventId;
}

