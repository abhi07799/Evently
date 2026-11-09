package com.abhishek.evently.service;


import com.abhishek.evently.dto.request.TicketStockRequestDTO;
import com.abhishek.evently.dto.response.TicketStockResponseDTO;
import com.abhishek.evently.entity.TicketStockEntity;
import com.abhishek.evently.exception.CustomException;
import com.abhishek.evently.exception.ResourceNotFoundException;
import com.abhishek.evently.repository.EventRepository;
import com.abhishek.evently.repository.TicketStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import com.abhishek.evently.entity.EventEntity;
import com.abhishek.evently.entity.UserEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Objects;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketStockService
{
    private final TicketStockRepository ticketStockRepository;
    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;
    private final CurrentUserService currentUserService;

    public TicketStockResponseDTO createTicketStock(
            TicketStockRequestDTO ticketStockRequestDTO,
            Jwt jwt,
            Authentication authentication) {

        // Only the event owner or ADMIN can create its ticket stock.
        validateEventManagementAccess(
                ticketStockRequestDTO.getEventId(), jwt, authentication);

        TicketStockEntity ticketStockEntity =
                modelMapper.map(ticketStockRequestDTO, TicketStockEntity.class);

        return modelMapper.map(
                ticketStockRepository.save(ticketStockEntity),
                TicketStockResponseDTO.class);
    }

    public List<TicketStockResponseDTO> getAllTicketStocks()
    {
        List<TicketStockEntity> ticketStockEntities = ticketStockRepository.findAll();

        return ticketStockEntities.stream()
                .map(ticketStockEntity -> modelMapper.map(ticketStockEntity, TicketStockResponseDTO.class))
                .toList();
    }

    public TicketStockResponseDTO getTicketStockById(Long ticketStockId)
    {
        return modelMapper.map(
                ticketStockRepository.findById(ticketStockId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No Ticket Stock found with id: " + ticketStockId)),
                TicketStockResponseDTO.class);
    }

    public List<TicketStockResponseDTO> getTicketStocksByEventId(Long eventId)
    {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Event found with id: " + eventId));

        List<TicketStockEntity> ticketStockEntities = ticketStockRepository.findByEventId(eventId);

        return ticketStockEntities.stream()
                .map(ticketStockEntity -> modelMapper.map(ticketStockEntity, TicketStockResponseDTO.class))
                .toList();
    }

    public TicketStockResponseDTO updateTicketStock(
            Long ticketStockId,
            TicketStockRequestDTO ticketStockRequestDTO,
            Jwt jwt,
            Authentication authentication) {

        TicketStockEntity ticketStockEntity = ticketStockRepository.findById(ticketStockId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket Stock found with id: " + ticketStockId));

        // Prevent moving existing ticket stock to another event.
        if (!ticketStockEntity.getEventId().equals(ticketStockRequestDTO.getEventId())) {
            throw new CustomException("Event can not be changed!");
        }

        // Verify ownership of the event associated with this ticket stock.
        validateEventManagementAccess(
                ticketStockEntity.getEventId(), jwt, authentication);

        ticketStockEntity.setTicketName(ticketStockRequestDTO.getTicketName());
        ticketStockEntity.setDescription(ticketStockRequestDTO.getDescription());
        ticketStockEntity.setTicketPrice(ticketStockRequestDTO.getTicketPrice());
        ticketStockEntity.setTotalTickets(ticketStockRequestDTO.getTotalTickets());

        return modelMapper.map(
                ticketStockRepository.save(ticketStockEntity),
                TicketStockResponseDTO.class);
    }

    public void deleteTicketStock(
            Long ticketStockId,
            Jwt jwt,
            Authentication authentication) {

        TicketStockEntity ticketStockEntity = ticketStockRepository.findById(ticketStockId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket Stock found with id: " + ticketStockId));

        // Only the event owner or ADMIN can delete its ticket stock.
        validateEventManagementAccess(
                ticketStockEntity.getEventId(), jwt, authentication);

        ticketStockRepository.delete(ticketStockEntity);
    }

    /**
     * Verifies that the authenticated user owns the event.
     * ADMIN users can manage ticket stock for any event.
     */
    private void validateEventManagementAccess(
            Long eventId,
            Jwt jwt,
            Authentication authentication) {

        UserEntity currentUser = currentUserService.getCurrentUser(jwt);

        EventEntity eventEntity = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Event found with id: " + eventId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !Objects.equals(
                currentUser.getId(), eventEntity.getOrganizerUserId())) {

            throw new AccessDeniedException(
                    "You are not allowed to manage ticket stock for this event");
        }
    }
}

