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

import com.abhishek.evently.entity.UserEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

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
    private final CurrentUserService currentUserService;

    public Page<TicketResponseDTO> getAllTickets(Pageable pageable)
    {
        Page<TicketEntity> ticketEntities = ticketRepository.findAll(pageable);

        return ticketEntities.map(ticketEntity -> modelMapper.map(ticketEntity, TicketResponseDTO.class));
    }

    public TicketResponseDTO getTicketById(
            Long ticketId,
            Jwt jwt,
            Authentication authentication) {

        // Resolve the authenticated Evently user.
        UserEntity currentUser = currentUserService.getCurrentUser(jwt);

        TicketEntity ticketEntity = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket found with id: " + ticketId));

        // ADMIN can access any ticket.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        // Other users can access only their own ticket.
        if (!isAdmin && !currentUser.getId().equals(ticketEntity.getPurchaserUserId())) {
            throw new AccessDeniedException("You are not allowed to view this ticket");
        }

        return modelMapper.map(ticketEntity, TicketResponseDTO.class);
    }

    public Page<TicketResponseDTO> getTicketsByTicketStockId(Long ticketStockId, Pageable pageable)
    {
        ticketStockRepository.findById(ticketStockId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket Stock found with id: " + ticketStockId));

        Page<TicketEntity> ticketEntities = ticketRepository.findByTicketStockId(ticketStockId, pageable);

        return ticketEntities.map(ticketEntity -> modelMapper.map(ticketEntity, TicketResponseDTO.class));
    }

    /**
     * Allows attendees to view only their own tickets.
     * ADMIN users can view tickets belonging to any user.
     */
    public Page<TicketResponseDTO> getTicketsByPurchaserUserId(
            Long purchaserUserId,
            Pageable pageable,
            Jwt jwt,
            Authentication authentication) {

        UserEntity currentUser = currentUserService.getCurrentUser(jwt);

        // Check whether the authenticated user has the ADMIN role.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        // Attendees cannot access another user's ticket history.
        if (!isAdmin && !currentUser.getId().equals(purchaserUserId)) {
            throw new AccessDeniedException(
                    "You are not allowed to view another user's tickets"
            );
        }

        // Verify the requested user exists (especially relevant for ADMIN).
        userRepository.findById(purchaserUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No User found with id: " + purchaserUserId));

        Page<TicketEntity> ticketEntities =
                ticketRepository.findByPurchaserUserId(purchaserUserId, pageable);

        return ticketEntities.map(
                ticketEntity -> modelMapper.map(ticketEntity, TicketResponseDTO.class)
        );
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


    /**
     * Purchases a ticket for the currently authenticated user.
     * The purchaser ID from the request body is never trusted.
     */
    @Transactional
    public TicketResponseDTO purchaseTicket(TicketRequestDTO ticketRequestDTO, Jwt jwt)
    {
        // Resolve the purchaser using the authenticated Keycloak identity.
        UserEntity currentUser = currentUserService.getCurrentUser(jwt);

        TicketStockEntity ticketStockEntity = ticketStockRepository.findByIdWithLock(
                        ticketRequestDTO.getTicketStockId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket Stock found with id: " + ticketRequestDTO.getTicketStockId()));

        long soldTickets = ticketRepository.countByTicketStockIdAndTicketStatus(
                ticketStockEntity.getId(), TicketStatusEnum.PURCHASED);

        if (soldTickets >= ticketStockEntity.getTotalTickets())
        {
            throw new CustomException("Sorry! No tickets available for this event.");
        }

        TicketEntity ticketEntity = new TicketEntity();

        ticketEntity.setTicketStockId(ticketStockEntity.getId());

        // Always assign the authenticated user's Evently ID.
        ticketEntity.setPurchaserUserId(currentUser.getId());

        ticketEntity.setTicketStatus(TicketStatusEnum.PURCHASED);

        TicketEntity savedTicketEntity = ticketRepository.save(ticketEntity);

        UUID qrCodeId = qrCodeService.generateQRCode(savedTicketEntity.getId()).getId();
        savedTicketEntity.setQrCodeId(qrCodeId);

        return modelMapper.map(
                ticketRepository.save(savedTicketEntity),
                TicketResponseDTO.class);
    }

    /**
     * Allows attendees to cancel only their own tickets.
     * ADMIN users can cancel any ticket.
     */
    @Transactional
    public TicketResponseDTO cancelTicket(
            Long ticketId,
            Jwt jwt,
            Authentication authentication)
    {
        UserEntity currentUser = currentUserService.getCurrentUser(jwt);

        TicketEntity ticketEntity = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket found with id: " + ticketId));

        // Check whether the authenticated user has the ADMIN role.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        // Prevent attendees from cancelling tickets owned by other users.
        if (!isAdmin && !currentUser.getId().equals(ticketEntity.getPurchaserUserId()))
        {
            throw new AccessDeniedException("You are not allowed to cancel this ticket");
        }

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
