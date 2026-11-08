package com.abhishek.evently.controller;

import com.abhishek.evently.dto.request.EventRequestDTO;
import com.abhishek.evently.dto.response.EventResponseDTO;
import com.abhishek.evently.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Event Controller", description = "Event management APIs")
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController
{

    private final EventService eventService;

    @Operation(
            summary = "Create a new event",
            description = "This endpoint creates a new event and returns the event response DTO."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Event created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EventResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organizer User Not Found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/createEvent")
    public ResponseEntity<EventResponseDTO> createEvent(
            @Valid @RequestBody EventRequestDTO eventRequestDTO) {

        EventResponseDTO response = eventService.createEvent(eventRequestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Fetch all events",
            description = "This endpoint returns a list of all event response DTOs."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "All Events fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EventResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Events Not Found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/allEvents")
    public ResponseEntity<Page<EventResponseDTO>> getAllEvents(@PageableDefault(size = 10, sort = "id") Pageable pageable) {

        return new ResponseEntity<>(
                eventService.getAllEvents(pageable),
                HttpStatus.OK
        );
    }

    @Operation(
            summary = "Fetch event by ID",
            description = "This endpoint fetches an event using its unique event ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Event fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EventResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event Not Found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponseDTO> getEventById(
            @PathVariable Long eventId) {

        return new ResponseEntity<>(
                eventService.getEventById(eventId),
                HttpStatus.OK
        );
    }

    @Operation(
            summary = "Fetch events by organizer user ID",
            description = "This endpoint fetches an event associated with the given organizer user ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Organizer Event fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EventResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No Event Found For This Organizer",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/organizer/{organizerUserId}")
    public ResponseEntity<Page<EventResponseDTO>> getEventByOrganizerUserId(
            @PathVariable Long organizerUserId, @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        return new ResponseEntity<>(
                eventService.getEventByOrganizerUserId(organizerUserId, pageable),
                HttpStatus.OK
        );
    }

    @Operation(
            summary = "Update an existing event",
            description = "This endpoint updates event details while ensuring that the organizer cannot be changed."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Event updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EventResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event or User Not Found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PutMapping("/{eventId}/organizer/{organizerUserId}")
    public ResponseEntity<EventResponseDTO> updateEvent(
            @PathVariable Long eventId,
            @PathVariable Long organizerUserId,
            @RequestBody EventRequestDTO eventRequestDTO) {

        EventResponseDTO response = eventService.updateEvent(
                eventId,
                organizerUserId,
                eventRequestDTO
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
