package com.abhishek.evently.service;

import com.abhishek.evently.dto.request.EventRequestDTO;
import com.abhishek.evently.dto.response.EventResponseDTO;
import com.abhishek.evently.entity.EventEntity;
import com.abhishek.evently.entity.UserEntity;
import com.abhishek.evently.entity.enums.EventStatusEnum;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import com.abhishek.evently.exception.ResourceNotFoundException;
import com.abhishek.evently.repository.EventRepository;
import com.abhishek.evently.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService
{
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    private final CurrentUserService currentUserService;

    /**
     * Creates an event using the identity of the authenticated user.
     * The organizer ID from the request body is never trusted.
     */
    // ORGANIZER can view only their own events; ADMIN can view any organizer's events.
    public Page<EventResponseDTO> getEventByOrganizerUserId(
            Long organizerUserId,
            Pageable pageable,
            Jwt jwt,
            Authentication authentication) {

        UserEntity currentUser = currentUserService.getCurrentUser(jwt);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUser.getId().equals(organizerUserId)) {
            throw new AccessDeniedException(
                    "You are not allowed to view another organizer's events");
        }

        Page<EventEntity> eventEntities =
                eventRepository.findByOrganizerUserId(organizerUserId, pageable);

        return eventEntities.map(event ->
                modelMapper.map(event, EventResponseDTO.class));
    }

    public Page<EventResponseDTO> getAllEvents(Pageable pageable)
    {
        Page<EventEntity> eventEntities = eventRepository.findAll(pageable);

        return eventEntities.map(eventEntity -> modelMapper.map(eventEntity, EventResponseDTO.class));
    }

    public EventResponseDTO getEventById(Long eventId)
    {
        return modelMapper.map(eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("No Event found with id: "+eventId)), EventResponseDTO.class);
    }

    public Page<EventResponseDTO> getEventByOrganizerUserId(Long organizerUserId, Pageable pageable)
    {
        Page<EventEntity> eventEntities = eventRepository.findByOrganizerUserId(organizerUserId, pageable);
        return eventEntities.map(eventEntity -> modelMapper.map(eventEntity, EventResponseDTO.class));
    }

    /**
     * Updates an event only when the authenticated user owns it.
     * ADMIN users can update any event.
     */
    public EventResponseDTO updateEvent(
            Long eventId,
            EventRequestDTO eventRequestDTO,
            Jwt jwt,
            Authentication authentication) {

        // Resolve the actual authenticated Evently user.
        UserEntity currentUser = currentUserService.getCurrentUser(jwt);

        // Fetch the event whose details are being updated.
        EventEntity eventEntity = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Event found with id: " + eventId
                ));

        // Check whether the authenticated user has the ADMIN role.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        // Non-admin users can update only events they own.
        if (!isAdmin && !currentUser.getId().equals(eventEntity.getOrganizerUserId())) {
            throw new AccessDeniedException("You are not allowed to update this event");
        }

        // Validate the assigned attendee and staff users.
        userRepository.findById(eventRequestDTO.getAttendeeUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No User found for attendee id " + eventRequestDTO.getAttendeeUserId()
                ));

        userRepository.findById(eventRequestDTO.getStaffUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No User found for staff id " + eventRequestDTO.getStaffUserId()
                ));

        // Update event details without changing its original organizer.
        eventEntity.setEventName(eventRequestDTO.getEventName());
        eventEntity.setDescription(eventRequestDTO.getDescription());
        eventEntity.setVenue(eventRequestDTO.getVenue());
        eventEntity.setEventStatus(EventStatusEnum.valueOf(eventRequestDTO.getEventStatus()));
        eventEntity.setEventStartTime(eventRequestDTO.getEventStartTime());
        eventEntity.setEventEndTime(eventRequestDTO.getEventEndTime());
        eventEntity.setTicketSalesStartTime(eventRequestDTO.getTicketSalesStartTime());
        eventEntity.setTicketSalesEndTime(eventRequestDTO.getTicketSalesEndTime());
        eventEntity.setAttendeeUserId(eventRequestDTO.getAttendeeUserId());
        eventEntity.setStaffUserId(eventRequestDTO.getStaffUserId());

        return modelMapper.map(eventRepository.save(eventEntity), EventResponseDTO.class);
    }
}
