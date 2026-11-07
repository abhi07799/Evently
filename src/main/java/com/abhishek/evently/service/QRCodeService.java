package com.abhishek.evently.service;

import com.abhishek.evently.dto.response.QRCodeResponseDTO;
import com.abhishek.evently.entity.QRCodeEntity;
import com.abhishek.evently.entity.enums.QRCodeValidationStatusEnum;
import com.abhishek.evently.exception.CustomException;
import com.abhishek.evently.exception.ResourceNotFoundException;
import com.abhishek.evently.repository.QRCodeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class QRCodeService
{

    private final QRCodeRepository qrCodeRepository;
    private final QRCodeWriter qrCodeWriter;
    private final ModelMapper modelMapper;

    private final static int QR_WIDTH = 300;
    private final static int QR_HEIGHT = 300;

    public QRCodeResponseDTO generateQRCode(Long ticketId)
    {
        try
        {
            UUID uuid = UUID.randomUUID();

            String qrCodeImage = generateQRCodeImage(uuid);

            QRCodeEntity qrCodeEntity = new QRCodeEntity();

            qrCodeEntity.setId(uuid);
            qrCodeEntity.setValidationStatus(QRCodeValidationStatusEnum.ACTIVE);
            qrCodeEntity.setQrCodeValue(qrCodeImage);
            qrCodeEntity.setTicketId(ticketId);

            return modelMapper.map(qrCodeRepository.save(qrCodeEntity), QRCodeResponseDTO.class);
        }
        catch (IOException | WriterException ex)
        {
            throw new CustomException("QR Code can not be generated! Please try again.", ex);
        }
    }

    public byte[] getQrCodeForPurchaserAndTicket(Long purchaserId, Long ticketId)
    {
        QRCodeEntity qrCodeEntity = qrCodeRepository.findByTicketIdAndPurchaserUserId(ticketId, purchaserId).orElseThrow(()-> new ResourceNotFoundException("No User or Ticket found for purchaser Id: " + purchaserId+" and Ticket Id: " + ticketId));
        try
        {
            return Base64.getDecoder().decode(qrCodeEntity.getQrCodeValue());
        }
        catch (Exception e)
        {
            throw new CustomException("QR Code can not be generated! Please try again.", e);
        }
    }

    private String generateQRCodeImage(UUID qrCodeId) throws WriterException, IOException
    {
        BitMatrix bitMatrix = qrCodeWriter.encode(
                qrCodeId.toString(),
                BarcodeFormat.QR_CODE,
                QR_WIDTH,
                QR_HEIGHT
        );

        BufferedImage qrCodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        try
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrCodeImage, "PNG", outputStream);

            byte[] imgBytes = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(imgBytes);
        }
        catch (Exception e)
        {
            throw new CustomException("QR Code can not be generated! Please try again.");
        }
    }
}
