package com.abhishek.evently.controller;

import com.abhishek.evently.dto.request.TicketStockRequestDTO;
import com.abhishek.evently.dto.response.TicketStockResponseDTO;
import com.abhishek.evently.service.TicketStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ticket-stocks")
@RequiredArgsConstructor
public class TicketStockController {

    private final TicketStockService ticketStockService;

    // Only ADMIN and ORGANIZER can create ticket stock.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<TicketStockResponseDTO> createTicketStock(
            @Valid @RequestBody TicketStockRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketStockService.createTicketStock(dto, jwt, authentication));
    }

    // Authenticated users can view ticket stocks.
    @GetMapping
    public ResponseEntity<List<TicketStockResponseDTO>> getAllTicketStocks() {
        return ResponseEntity.ok(ticketStockService.getAllTicketStocks());
    }

    @GetMapping("/{ticketStockId}")
    public ResponseEntity<TicketStockResponseDTO> getTicketStockById(
            @PathVariable Long ticketStockId) {

        return ResponseEntity.ok(ticketStockService.getTicketStockById(ticketStockId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketStockResponseDTO>> getTicketStocksByEventId(
            @PathVariable Long eventId) {

        return ResponseEntity.ok(ticketStockService.getTicketStocksByEventId(eventId));
    }

    // ADMIN can update any stock; ORGANIZER can update only their own event's stock.
    @PutMapping("/{ticketStockId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<TicketStockResponseDTO> updateTicketStock(
            @PathVariable Long ticketStockId,
            @Valid @RequestBody TicketStockRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return ResponseEntity.ok(
                ticketStockService.updateTicketStock(ticketStockId, dto, jwt, authentication));
    }

    // ADMIN can delete any stock; ORGANIZER can delete only their own event's stock.
    @DeleteMapping("/{ticketStockId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<Void> deleteTicketStock(
            @PathVariable Long ticketStockId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        ticketStockService.deleteTicketStock(ticketStockId, jwt, authentication);
        return ResponseEntity.noContent().build();
    }
}