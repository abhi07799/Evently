package com.abhishek.evently.dto.response;


import com.abhishek.evently.entity.enums.EventStatusEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventResponseDTO
{
    private Long id;

    private String eventName;

    private String description;

    private String venue;

    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;

    private EventStatusEnum eventStatus;

    private LocalDateTime ticketSalesStartTime;
    private LocalDateTime ticketSalesEndTime;

    private Long organizerUserId;

    private Long attendeeUserId;

    private Long staffUserId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
