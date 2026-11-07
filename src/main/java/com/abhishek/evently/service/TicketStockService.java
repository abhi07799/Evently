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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketStockService
{
    private final TicketStockRepository ticketStockRepository;
    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;

    public TicketStockResponseDTO createTicketStock(TicketStockRequestDTO ticketStockRequestDTO)
    {
        eventRepository.findById(ticketStockRequestDTO.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Event found with id: " + ticketStockRequestDTO.getEventId()));

        TicketStockEntity ticketStockEntity = modelMapper.map(ticketStockRequestDTO, TicketStockEntity.class);

        return modelMapper.map(ticketStockRepository.save(ticketStockEntity), TicketStockResponseDTO.class);
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

    public TicketStockResponseDTO updateTicketStock(Long ticketStockId, TicketStockRequestDTO ticketStockRequestDTO)
    {
        eventRepository.findById(ticketStockRequestDTO.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Event found with id: " + ticketStockRequestDTO.getEventId()));

        TicketStockEntity ticketStockEntity = ticketStockRepository.findById(ticketStockId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket Stock found with id: " + ticketStockId));

        if (!ticketStockEntity.getEventId().equals(ticketStockRequestDTO.getEventId()))
        {
            throw new CustomException("Event can not be changed!");
        }

        ticketStockEntity.setTicketName(ticketStockRequestDTO.getTicketName());
        ticketStockEntity.setDescription(ticketStockRequestDTO.getDescription());
        ticketStockEntity.setTicketPrice(ticketStockRequestDTO.getTicketPrice());
        ticketStockEntity.setTotalTickets(ticketStockRequestDTO.getTotalTickets());

        return modelMapper.map(ticketStockRepository.save(ticketStockEntity), TicketStockResponseDTO.class);
    }

    public void deleteTicketStock(Long ticketStockId)
    {
        TicketStockEntity ticketStockEntity = ticketStockRepository.findById(ticketStockId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Ticket Stock found with id: " + ticketStockId));

        ticketStockRepository.delete(ticketStockEntity);
    }
}

