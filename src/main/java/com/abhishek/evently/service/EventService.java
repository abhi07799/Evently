package com.abhishek.evently.service;

import com.abhishek.evently.dto.request.EventRequestDTO;
import com.abhishek.evently.dto.response.EventResponseDTO;
import com.abhishek.evently.entity.EventEntity;
import com.abhishek.evently.entity.enums.EventStatusEnum;
import com.abhishek.evently.exception.CustomException;
import com.abhishek.evently.exception.ResourceNotFoundException;
import com.abhishek.evently.repository.EventRepository;
import com.abhishek.evently.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public EventResponseDTO createEvent(EventRequestDTO eventRequestDTO)
    {
        userRepository.findById(eventRequestDTO.getOrganizerUserId()).orElseThrow(()-> new ResourceNotFoundException("No User found for id " + eventRequestDTO.getOrganizerUserId()));

        EventEntity eventEntity = modelMapper.map(eventRequestDTO, EventEntity.class);

        return modelMapper.map(eventRepository.save(eventEntity), EventResponseDTO.class);
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

    public EventResponseDTO updateEvent(Long eventId, Long organizerUserId, EventRequestDTO eventRequestDTO)
    {
        userRepository.findById(eventRequestDTO.getOrganizerUserId()).orElseThrow(()-> new ResourceNotFoundException("No User found for id " + organizerUserId));
        userRepository.findById(eventRequestDTO.getAttendeeUserId()).orElseThrow(()-> new ResourceNotFoundException("No User found for attendee id " + eventRequestDTO.getAttendeeUserId()));
        userRepository.findById(eventRequestDTO.getStaffUserId()).orElseThrow(()-> new ResourceNotFoundException("No User found for staff id " + eventRequestDTO.getStaffUserId()));
        EventEntity eventEntity = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("No Event found with id: "+eventId));

        if(!organizerUserId.equals(eventRequestDTO.getOrganizerUserId()))
        {
            throw new CustomException("Organizer can not be changed!");
        }

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
