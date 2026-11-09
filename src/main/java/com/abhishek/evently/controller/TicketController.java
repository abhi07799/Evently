
package com.abhishek.evently.controller;

import com.abhishek.evently.dto.request.TicketRequestDTO;
import com.abhishek.evently.dto.response.TicketResponseDTO;
import com.abhishek.evently.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket Controller", description = "APIs for managing tickets")
public class TicketController {

    private final TicketService ticketService;

    @Operation(
            summary = "Get all tickets",
            description = "This endpoint retrieves all tickets with pagination and sorting support."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tickets retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/allTickets")
    public ResponseEntity<Page<TicketResponseDTO>> getAllTickets(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        return new ResponseEntity<>(ticketService.getAllTickets(pageable), HttpStatus.OK);
    }


    @Operation(
            summary = "Get ticket by ID",
            description = "This endpoint retrieves ticket details using the ticket ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ticket retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDTO> getTicketById(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return ResponseEntity.ok(
                ticketService.getTicketById(ticketId, jwt, authentication));
    }


    @Operation(
            summary = "Get tickets by ticket stock ID",
            description = "This endpoint retrieves all tickets associated with a particular ticket stock ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tickets retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket Stock not found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stock/{ticketStockId}")
    public ResponseEntity<Page<TicketResponseDTO>> getTicketsByTicketStockId(
            @PathVariable Long ticketStockId, @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        return new ResponseEntity<>(ticketService.getTicketsByTicketStockId(ticketStockId, pageable), HttpStatus.OK);
    }


    @Operation(
            summary = "Get tickets by purchaser user ID",
            description = "This endpoint retrieves all tickets purchased by a particular user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tickets retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    // Only attendees and admins can access purchaser ticket history.
    @PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
    @GetMapping("/purchaser/{purchaserUserId}")
    public ResponseEntity<Page<TicketResponseDTO>> getTicketsByPurchaserUserId(
            @PathVariable Long purchaserUserId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return new ResponseEntity<>(
                ticketService.getTicketsByPurchaserUserId(
                        purchaserUserId, pageable, jwt, authentication
                ),
                HttpStatus.OK
        );
    }


    @Operation(
            summary = "Update ticket",
            description = "This endpoint updates the ticket status. Ticket stock and purchaser cannot be changed."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ticket updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket, Ticket Stock or User not found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDTO> updateTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketRequestDTO ticketRequestDTO) {

        return new ResponseEntity<>(
                ticketService.updateTicket(ticketId, ticketRequestDTO), HttpStatus.OK);
    }


    @Operation(
            summary = "Purchase a ticket",
            description = "This endpoint purchases a ticket for the specified ticket stock and returns the purchased ticket details."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Ticket purchased successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket Stock or User not found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/purchase")
    public ResponseEntity<TicketResponseDTO> purchaseTicket(
            @Valid @RequestBody TicketRequestDTO ticketRequestDTO, @AuthenticationPrincipal Jwt jwt) {

        return new ResponseEntity<>(ticketService.purchaseTicket(ticketRequestDTO, jwt), HttpStatus.CREATED);
    }


    @Operation(
            summary = "Cancel a ticket",
            description = "This endpoint cancels a purchased ticket using its ticket ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ticket cancelled successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket not found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PatchMapping("/{ticketId}/cancel")
    public ResponseEntity<TicketResponseDTO> cancelTicket(
            @PathVariable Long ticketId, @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return new ResponseEntity<>(ticketService.cancelTicket(ticketId, jwt, authentication), HttpStatus.OK);
    }
}
