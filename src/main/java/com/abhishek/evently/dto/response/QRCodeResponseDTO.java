package com.abhishek.evently.dto.response;

import com.abhishek.evently.entity.enums.QRCodeValidationStatusEnum;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QRCodeResponseDTO
{
    private UUID id;

    private QRCodeValidationStatusEnum validationStatus;

    private String qrCodeValue;

    private Long ticketId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
