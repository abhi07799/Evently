package com.abhishek.evently.entity;

import com.abhishek.evently.entity.enums.EventStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events_tbl")
public class EventEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventName;

    private String description;

    private String venue;

    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;

    @Enumerated(EnumType.STRING)
    private EventStatusEnum eventStatus;

    private LocalDateTime ticketSalesStartTime;
    private LocalDateTime ticketSalesEndTime;

    private Long organizerUserId;

    private Long attendeeUserId;

    private Long staffUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
