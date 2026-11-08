package com.abhishek.evently.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventRequestDTO
{
    @NotBlank(message = "Event name is required")
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String eventName;

    @NotBlank(message = "Event description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Event venue is required")
    @Size(min = 3, max = 200, message = "Venue must be between 3 and 200 characters")
    private String venue;

    @NotNull(message = "Event start time is required")
    @Future(message = "Event start time must be in the future")
    private LocalDateTime eventStartTime;

    @NotNull(message = "Event end time is required")
    private LocalDateTime eventEndTime;

    @NotBlank(message = "Event status is required")
    @Pattern(
            regexp = "DRAFT|PUBLISHED|COMPLETED|CANCELLED",
            message = "Invalid event status"
    )
    private String eventStatus;

    @NotNull(message = "Ticket sales start time is required")
    private LocalDateTime ticketSalesStartTime;

    @NotNull(message = "Ticket sales end time is required")
    private LocalDateTime ticketSalesEndTime;

    @NotNull(message = "Organizer user ID is required")
    @Positive(message = "Organizer user ID must be positive")
    private Long organizerUserId;

    @NotNull(message = "Attendee user ID is required")
    @Positive(message = "Attendee user ID must be positive")
    private Long attendeeUserId;

    @NotNull(message = "Staff user ID is required")
    @Positive(message = "Staff user ID must be positive")
    private Long staffUserId;
}
