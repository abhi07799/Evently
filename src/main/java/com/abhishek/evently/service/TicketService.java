package com.abhishek.evently.service;


import com.abhishek.evently.dto.request.TicketRequestDTO;
import com.abhishek.evently.dto.response.TicketResponseDTO;
import com.abhishek.evently.entity.TicketEntity;
import com.abhishek.evently.entity.TicketStockEntity;
import com.abhishek.evently.entity.enums.TicketStatusEnum;
import com.abhishek.evently.exception.CustomException;
import com.abhishek.evently.exception.ResourceNotFoundException;
import com.abhishek.evently.repository.TicketRepository;
import com.abhishek.evently.repository.TicketStockRepository;
import com.abhishek.evently.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService
{
    private final TicketRepository ticketRepository;
    private final TicketStockRepository ticketStockRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final QRCodeService qrCodeService;

    public Page<TicketResponseDTO> getAllTickets(Pageable pageable)
    {
        Page<TicketEntity> ticketEntities = ticketRepository.findAll(pageable);

        return ticketEntities.map(ticketEntity -> modelMapper.map(ticketEntity, TicketResponseDTO.class));
    }

    public TicketResponseDTO getTicketById(Long ticketId)
    {
        return modelMapper.map(
                ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No Ticket found with id: " + ticketId)),
                TicketResponseDTO.class);
    }

    public Page<TicketResponseDTO> getTicketsByTicketStockId(Long ticketStockId, Pageable pageable)
    {
        ticketStockRepository.findById(ticketStockId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket Stock found with id: " + ticketStockId));

        Page<TicketEntity> ticketEntities = ticketRepository.findByTicketStockId(ticketStockId, pageable);

        return ticketEntities.map(ticketEntity -> modelMapper.map(ticketEntity, TicketResponseDTO.class));
    }

    public Page<TicketResponseDTO> getTicketsByPurchaserUserId(Long purchaserUserId, Pageable pageable)
    {
        userRepository.findById(purchaserUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No User found with id: " + purchaserUserId));

        Page<TicketEntity> ticketEntities = ticketRepository.findByPurchaserUserId(purchaserUserId, pageable);

        return ticketEntities.map(ticketEntity -> modelMapper.map(ticketEntity, TicketResponseDTO.class));
    }

    public TicketResponseDTO updateTicket(Long ticketId, TicketRequestDTO ticketRequestDTO)
    {
        TicketEntity ticketEntity = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket found with id: " + ticketId));

        ticketStockRepository.findById(ticketRequestDTO.getTicketStockId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket Stock found with id: " + ticketRequestDTO.getTicketStockId()));

        userRepository.findById(ticketRequestDTO.getPurchaserUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No User found with id: " + ticketRequestDTO.getPurchaserUserId()));

        if (!ticketEntity.getTicketStockId().equals(ticketRequestDTO.getTicketStockId()))
        {
            throw new CustomException("Ticket Stock can not be changed!");
        }

        if (!ticketEntity.getPurchaserUserId().equals(ticketRequestDTO.getPurchaserUserId()))
        {
            throw new CustomException("Ticket purchaser can not be changed!");
        }

        ticketEntity.setTicketStatus(TicketStatusEnum.valueOf(ticketRequestDTO.getTicketStatus()));

        return modelMapper.map(ticketRepository.save(ticketEntity), TicketResponseDTO.class);
    }


    @Transactional
    public TicketResponseDTO purchaseTicket(TicketRequestDTO ticketRequestDTO)
    {
        TicketStockEntity ticketStockEntity = ticketStockRepository.findByIdWithLock(
                        ticketRequestDTO.getTicketStockId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket Stock found with id: " + ticketRequestDTO.getTicketStockId()));

        userRepository.findById(ticketRequestDTO.getPurchaserUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No User found with id: " + ticketRequestDTO.getPurchaserUserId()));

        long soldTickets = ticketRepository.countByTicketStockIdAndTicketStatus(
                ticketStockEntity.getId(), TicketStatusEnum.PURCHASED);

        if (soldTickets >= ticketStockEntity.getTotalTickets())
        {
            throw new CustomException("Sorry! No tickets available for this event.");
        }

        TicketEntity ticketEntity = new TicketEntity();

        ticketEntity.setTicketStockId(ticketStockEntity.getId());
        ticketEntity.setPurchaserUserId(ticketRequestDTO.getPurchaserUserId());
        ticketEntity.setTicketStatus(TicketStatusEnum.PURCHASED);

        TicketEntity savedTicketEntity = ticketRepository.save(ticketEntity);

        UUID qrCodeId = qrCodeService.generateQRCode(savedTicketEntity.getId()).getId();
        savedTicketEntity.setQrCodeId(qrCodeId);

        return modelMapper.map(
                ticketRepository.save(savedTicketEntity),
                TicketResponseDTO.class);
    }


    @Transactional
    public TicketResponseDTO cancelTicket(Long ticketId)
    {
        TicketEntity ticketEntity = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket found with id: " + ticketId));

        if (ticketEntity.getTicketStatus() != TicketStatusEnum.PURCHASED)
        {
            throw new CustomException("Only purchased tickets can be cancelled!");
        }

        ticketEntity.setTicketStatus(TicketStatusEnum.CANCELLED);

        return modelMapper.map(
                ticketRepository.save(ticketEntity),
                TicketResponseDTO.class);
    }

}
