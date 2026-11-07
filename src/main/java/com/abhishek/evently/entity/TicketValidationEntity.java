package com.abhishek.evently.entity;

import com.abhishek.evently.entity.enums.TicketValidationMethodEnum;
import com.abhishek.evently.entity.enums.TicketValidationStatusEnum;
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
@Table(name = "tickets_validation_tbl")
public class TicketValidationEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TicketValidationStatusEnum ticketValidationStatus;

    @Enumerated(EnumType.STRING)
    private TicketValidationMethodEnum ticketValidationMethod;

    private Long ticketId;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
