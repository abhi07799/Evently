package com.abhishek.evently.entity;

import com.abhishek.evently.entity.enums.QRCodeValidationStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "qr_codes_tbl")
public class QRCodeEntity
{
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private QRCodeValidationStatusEnum validationStatus;

    private String qrCodeValue;

    private Long ticketId;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
