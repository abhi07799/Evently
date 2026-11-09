# Evently – Keycloak & RBAC Code Reference

> Ye companion document hai. Implementation steps wali purani documentation ko change nahi kiya gaya. Is file mein conversation ke dauran implement/replace kiye gaye important Java code snippets hain. Imports intentionally omit kiye gaye hain. Snippets ko existing classes ke indicated methods/sections mein integrate karna hai; har snippet standalone complete class nahi hai.

## 1. Current authenticated user ko Evently profile se resolve karna

**File:** service/CurrentUserService.java

~~~java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentUserService {

    private final UserRepository userRepository;

    /**
     * JWT ke subject (Keycloak UUID) se Evently user profile resolve karta hai.
     */
    public UserEntity getCurrentUser(Jwt jwt) {
        // Keycloak UUID JWT ke sub claim mein hota hai.
        String keycloakUserId = jwt.getSubject();

        return userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user profile not found"
                ));
    }
}
~~~

**UserRepository method:**

~~~java
Optional<UserEntity> findByKeycloakUserId(String keycloakUserId);
~~~

## 2. Event endpoints par role restrictions

Event create/update methods ke upar:

~~~java
// ADMIN aur ORGANIZER event create/update kar sakte hain.
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
~~~

Actual event ownership check service layer mein hota hai.

## 3. Event creation – authenticated organizer assign karna

**EventService – createEvent method:**

~~~java
public EventResponseDTO createEvent(EventRequestDTO eventRequestDTO, Jwt jwt) {

    // Request body ke organizer ID par trust nahi karna.
    UserEntity currentUser = currentUserService.getCurrentUser(jwt);

    EventEntity eventEntity = modelMapper.map(eventRequestDTO, EventEntity.class);

    // Logged-in user hi event ka organizer hoga.
    eventEntity.setOrganizerUserId(currentUser.getId());

    return modelMapper.map(
            eventRepository.save(eventEntity),
            EventResponseDTO.class
    );
}
~~~

**EventController – create method:**

~~~java
@PostMapping("/createEvent")
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public ResponseEntity<EventResponseDTO> createEvent(
        @Valid @RequestBody EventRequestDTO eventRequestDTO,
        @AuthenticationPrincipal Jwt jwt) {

    EventResponseDTO response = eventService.createEvent(eventRequestDTO, jwt);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
}
~~~

## 4. Event update – ADMIN override + organizer ownership

**EventService – updateEvent method:**

~~~java
public EventResponseDTO updateEvent(
        Long eventId,
        EventRequestDTO eventRequestDTO,
        Jwt jwt,
        Authentication authentication) {

    UserEntity currentUser = currentUserService.getCurrentUser(jwt);

    EventEntity eventEntity = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "No Event found with id: " + eventId));

    // ADMIN kisi bhi event ko update kar sakta hai.
    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority ->
                    authority.getAuthority().equals("ROLE_ADMIN"));

    // ORGANIZER sirf apna event update kar sakta hai.
    if (!isAdmin && !currentUser.getId().equals(eventEntity.getOrganizerUserId())) {
        throw new AccessDeniedException(
                "You are not allowed to update this event");
    }

    // Existing references validate karo.
    userRepository.findById(eventRequestDTO.getAttendeeUserId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    "No User found for attendee id "
                            + eventRequestDTO.getAttendeeUserId()));

    userRepository.findById(eventRequestDTO.getStaffUserId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    "No User found for staff id "
                            + eventRequestDTO.getStaffUserId()));

    // Event details update karo; original organizer ID change mat karo.
    eventEntity.setEventName(eventRequestDTO.getEventName());
    eventEntity.setDescription(eventRequestDTO.getDescription());
    eventEntity.setVenue(eventRequestDTO.getVenue());
    eventEntity.setEventStatus(
            EventStatusEnum.valueOf(eventRequestDTO.getEventStatus()));
    eventEntity.setEventStartTime(eventRequestDTO.getEventStartTime());
    eventEntity.setEventEndTime(eventRequestDTO.getEventEndTime());
    eventEntity.setTicketSalesStartTime(eventRequestDTO.getTicketSalesStartTime());
    eventEntity.setTicketSalesEndTime(eventRequestDTO.getTicketSalesEndTime());
    eventEntity.setAttendeeUserId(eventRequestDTO.getAttendeeUserId());
    eventEntity.setStaffUserId(eventRequestDTO.getStaffUserId());

    return modelMapper.map(
            eventRepository.save(eventEntity),
            EventResponseDTO.class);
}
~~~

**EventController – update method:**

~~~java
@PutMapping("/{eventId}/organizer/{organizerUserId}")
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public ResponseEntity<EventResponseDTO> updateEvent(
        @PathVariable Long eventId,
        @PathVariable Long organizerUserId,
        @RequestBody EventRequestDTO eventRequestDTO,
        @AuthenticationPrincipal Jwt jwt,
        Authentication authentication) {

    EventResponseDTO response = eventService.updateEvent(
            eventId,
            eventRequestDTO,
            jwt,
            authentication
    );

    return new ResponseEntity<>(response, HttpStatus.OK);
}
~~~

Note: organizerUserId path variable backward compatibility ke liye route mein reh sakta hai; authorization JWT aur event ownership se hoti hai, path value se nahi.

## 5. Ticket purchase – purchaser JWT se set karna

**TicketController role restriction:**

~~~java
@PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
~~~

**TicketService – purchaseTicket method:**

~~~java
@Transactional
public TicketResponseDTO purchaseTicket(TicketRequestDTO ticketRequestDTO, Jwt jwt) {

    // Purchaser ko request body se trust nahi karna.
    UserEntity currentUser = currentUserService.getCurrentUser(jwt);

    // Concurrent purchases ke liye existing pessimistic lock preserve hai.
    TicketStockEntity ticketStockEntity =
            ticketStockRepository.findByIdWithLock(ticketRequestDTO.getTicketStockId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No Ticket Stock found with id: "
                                    + ticketRequestDTO.getTicketStockId()));

    long soldTickets = ticketRepository.countByTicketStockIdAndTicketStatus(
            ticketStockEntity.getId(),
            TicketStatusEnum.PURCHASED);

    if (soldTickets >= ticketStockEntity.getTotalTickets()) {
        throw new CustomException(
                "Sorry! No tickets available for this event.");
    }

    TicketEntity ticketEntity = new TicketEntity();

    ticketEntity.setTicketStockId(ticketStockEntity.getId());

    // Purchaser authenticated Evently user hoga.
    ticketEntity.setPurchaserUserId(currentUser.getId());
    ticketEntity.setTicketStatus(TicketStatusEnum.PURCHASED);

    TicketEntity savedTicketEntity = ticketRepository.save(ticketEntity);

    // Existing QR generation flow preserve hai.
    UUID qrCodeId = qrCodeService.generateQRCode(savedTicketEntity.getId()).getId();
    savedTicketEntity.setQrCodeId(qrCodeId);

    return modelMapper.map(
            ticketRepository.save(savedTicketEntity),
            TicketResponseDTO.class);
}
~~~

**TicketController – purchase method:**

~~~java
@PostMapping("/purchase")
@PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
public ResponseEntity<TicketResponseDTO> purchaseTicket(
        @Valid @RequestBody TicketRequestDTO ticketRequestDTO,
        @AuthenticationPrincipal Jwt jwt) {

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ticketService.purchaseTicket(ticketRequestDTO, jwt));
}
~~~

## 6. Ticket cancellation – purchaser ownership

**TicketController role restriction:**

~~~java
@PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
~~~

**TicketService – cancelTicket method:**

~~~java
@Transactional
public TicketResponseDTO cancelTicket(
        Long ticketId,
        Jwt jwt,
        Authentication authentication) {

    UserEntity currentUser = currentUserService.getCurrentUser(jwt);

    TicketEntity ticketEntity = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "No Ticket found with id: " + ticketId));

    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority ->
                    authority.getAuthority().equals("ROLE_ADMIN"));

    // ADMIN any ticket cancel kar sakta hai; attendee sirf apna.
    if (!isAdmin
            && !currentUser.getId().equals(ticketEntity.getPurchaserUserId())) {
        throw new AccessDeniedException(
                "You are not allowed to cancel this ticket");
    }

    if (ticketEntity.getTicketStatus() != TicketStatusEnum.PURCHASED) {
        throw new CustomException(
                "Only purchased tickets can be cancelled!");
    }

    ticketEntity.setTicketStatus(TicketStatusEnum.CANCELLED);

    return modelMapper.map(
            ticketRepository.save(ticketEntity),
            TicketResponseDTO.class);
}
~~~

**TicketController – cancel method:**

~~~java
@PatchMapping("/{ticketId}/cancel")
@PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
public ResponseEntity<TicketResponseDTO> cancelTicket(
        @PathVariable Long ticketId,
        @AuthenticationPrincipal Jwt jwt,
        Authentication authentication) {

    return ResponseEntity.ok(
            ticketService.cancelTicket(ticketId, jwt, authentication));
}
~~~

## 7. Purchaser-wise ticket listing – owner/ADMIN only

**TicketService – getTicketsByPurchaserUserId method:**

~~~java
public Page<TicketResponseDTO> getTicketsByPurchaserUserId(
        Long purchaserUserId,
        Pageable pageable,
        Jwt jwt,
        Authentication authentication) {

    UserEntity currentUser = currentUserService.getCurrentUser(jwt);

    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority ->
                    authority.getAuthority().equals("ROLE_ADMIN"));

    if (!isAdmin && !currentUser.getId().equals(purchaserUserId)) {
        throw new AccessDeniedException(
                "You are not allowed to view another user's tickets");
    }

    userRepository.findById(purchaserUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "No User found with id: " + purchaserUserId));

    Page<TicketEntity> ticketEntities =
            ticketRepository.findByPurchaserUserId(purchaserUserId, pageable);

    return ticketEntities.map(ticketEntity ->
            modelMapper.map(ticketEntity, TicketResponseDTO.class));
}
~~~

**TicketController – purchaser listing method:**

~~~java
@GetMapping("/purchaser/{purchaserUserId}")
@PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
public ResponseEntity<Page<TicketResponseDTO>> getTicketsByPurchaserUserId(
        @PathVariable Long purchaserUserId,
        @PageableDefault(size = 10, sort = "id") Pageable pageable,
        @AuthenticationPrincipal Jwt jwt,
        Authentication authentication) {

    return ResponseEntity.ok(
            ticketService.getTicketsByPurchaserUserId(
                    purchaserUserId, pageable, jwt, authentication));
}
~~~

## 8. Ticket stock – event ownership validation

**TicketStockService – shared ownership helper:**

~~~java
private void validateEventManagementAccess(
        Long eventId,
        Jwt jwt,
        Authentication authentication) {

    UserEntity currentUser = currentUserService.getCurrentUser(jwt);

    EventEntity eventEntity = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "No Event found with id: " + eventId));

    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority ->
                    authority.getAuthority().equals("ROLE_ADMIN"));

    if (!isAdmin
            && !Objects.equals(
                    currentUser.getId(),
                    eventEntity.getOrganizerUserId())) {
        throw new AccessDeniedException(
                "You are not allowed to manage ticket stock for this event");
    }
}
~~~

**Create ticket stock:**

~~~java
public TicketStockResponseDTO createTicketStock(
        TicketStockRequestDTO dto,
        Jwt jwt,
        Authentication authentication) {

    // User ko requested event manage karne ka access hona chahiye.
    validateEventManagementAccess(dto.getEventId(), jwt, authentication);

    TicketStockEntity entity = modelMapper.map(dto, TicketStockEntity.class);

    return modelMapper.map(
            ticketStockRepository.save(entity),
            TicketStockResponseDTO.class);
}
~~~

**Update ticket stock:**

~~~java
public TicketStockResponseDTO updateTicketStock(
        Long ticketStockId,
        TicketStockRequestDTO dto,
        Jwt jwt,
        Authentication authentication) {

    TicketStockEntity entity = ticketStockRepository.findById(ticketStockId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "No Ticket Stock found with id: " + ticketStockId));

    // Existing stock ko kisi doosre event mein transfer nahi kar sakte.
    if (!entity.getEventId().equals(dto.getEventId())) {
        throw new CustomException("Event can not be changed!");
    }

    validateEventManagementAccess(entity.getEventId(), jwt, authentication);

    entity.setTicketName(dto.getTicketName());
    entity.setDescription(dto.getDescription());
    entity.setTicketPrice(dto.getTicketPrice());
    entity.setTotalTickets(dto.getTotalTickets());

    return modelMapper.map(
            ticketStockRepository.save(entity),
            TicketStockResponseDTO.class);
}
~~~

**Delete ticket stock:**

~~~java
public void deleteTicketStock(
        Long ticketStockId,
        Jwt jwt,
        Authentication authentication) {

    TicketStockEntity entity = ticketStockRepository.findById(ticketStockId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "No Ticket Stock found with id: " + ticketStockId));

    validateEventManagementAccess(entity.getEventId(), jwt, authentication);

    ticketStockRepository.delete(entity);
}
~~~

## 9. TicketStockController – endpoints

**File:** controller/TicketStockController.java

~~~java
@RestController
@RequestMapping("/ticket-stocks")
@RequiredArgsConstructor
public class TicketStockController {

    private final TicketStockService ticketStockService;

    // ADMIN/ORGANIZER create; event ownership service layer par validate hoti hai.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<TicketStockResponseDTO> createTicketStock(
            @Valid @RequestBody TicketStockRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketStockService.createTicketStock(
                        dto, jwt, authentication));
    }

    // Authenticated users ticket stock browse kar sakte hain.
    @GetMapping
    public ResponseEntity<List<TicketStockResponseDTO>> getAllTicketStocks() {
        return ResponseEntity.ok(ticketStockService.getAllTicketStocks());
    }

    @GetMapping("/{ticketStockId}")
    public ResponseEntity<TicketStockResponseDTO> getTicketStockById(
            @PathVariable Long ticketStockId) {

        return ResponseEntity.ok(
                ticketStockService.getTicketStockById(ticketStockId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketStockResponseDTO>> getTicketStocksByEventId(
            @PathVariable Long eventId) {

        return ResponseEntity.ok(
                ticketStockService.getTicketStocksByEventId(eventId));
    }

    // ADMIN any event; ORGANIZER only own event (service ownership check).
    @PutMapping("/{ticketStockId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<TicketStockResponseDTO> updateTicketStock(
            @PathVariable Long ticketStockId,
            @Valid @RequestBody TicketStockRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return ResponseEntity.ok(
                ticketStockService.updateTicketStock(
                        ticketStockId, dto, jwt, authentication));
    }

    // ADMIN any event; ORGANIZER only own event (service ownership check).
    @DeleteMapping("/{ticketStockId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<Void> deleteTicketStock(
            @PathVariable Long ticketStockId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        ticketStockService.deleteTicketStock(
                ticketStockId, jwt, authentication);

        return ResponseEntity.noContent().build();
    }
}
~~~

## 10. Individual ticket details – owner/ADMIN only

**TicketService – getTicketById method:**

~~~java
public TicketResponseDTO getTicketById(
        Long ticketId,
        Jwt jwt,
        Authentication authentication) {

    UserEntity currentUser = currentUserService.getCurrentUser(jwt);

    TicketEntity ticketEntity = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "No Ticket found with id: " + ticketId));

    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority ->
                    authority.getAuthority().equals("ROLE_ADMIN"));

    if (!isAdmin
            && !currentUser.getId().equals(ticketEntity.getPurchaserUserId())) {
        throw new AccessDeniedException(
                "You are not allowed to view this ticket");
    }

    return modelMapper.map(ticketEntity, TicketResponseDTO.class);
}
~~~

**TicketController – individual ticket endpoint:**

~~~java
@GetMapping("/{ticketId}")
public ResponseEntity<TicketResponseDTO> getTicketById(
        @PathVariable Long ticketId,
        @AuthenticationPrincipal Jwt jwt,
        Authentication authentication) {

    return ResponseEntity.ok(
            ticketService.getTicketById(ticketId, jwt, authentication));
}
~~~

## 11. Sensitive ticket listing restrictions

**TicketController annotations:**

~~~java
// All tickets ka listing sirf ADMIN dekh sakta hai.
@GetMapping("/allTickets")
@PreAuthorize("hasRole('ADMIN')")
~~~

~~~java
// Ek stock ke saare purchasers/tickets ki listing sirf ADMIN dekh sakta hai.
@GetMapping("/stock/{ticketStockId}")
@PreAuthorize("hasRole('ADMIN')")
~~~

## 12. Manual ticket status update – ADMIN only

**TicketController:**

~~~java
// Manual ticket status change sirf ADMIN kar sakta hai.
@PutMapping("/{ticketId}")
@PreAuthorize("hasRole('ADMIN')")
~~~

## 13. Organizer-wise event listing – owner/ADMIN only

**EventService – getEventByOrganizerUserId method:**

~~~java
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
~~~

**EventController – organizer listing endpoint:**

~~~java
@GetMapping("/organizer/{organizerUserId}")
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
public ResponseEntity<Page<EventResponseDTO>> getEventByOrganizerUserId(
        @PathVariable Long organizerUserId,
        @PageableDefault(size = 10, sort = "id") Pageable pageable,
        @AuthenticationPrincipal Jwt jwt,
        Authentication authentication) {

    return ResponseEntity.ok(
            eventService.getEventByOrganizerUserId(
                    organizerUserId, pageable, jwt, authentication));
}
~~~

## 14. Security configuration – important points

SecurityConfig mein ye behavior configure kiya gaya tha:

- @EnableMethodSecurity enable.
- Session management stateless.
- CSRF disabled for stateless JWT API.
- POST /auth/register permitAll.
- Swagger/OpenAPI routes permitAll.
- Baaki requests authenticated.
- OAuth2 JWT resource server enabled.
- Keycloak realm_access.roles se roles read karke ROLE_<ROLE> Spring authorities mein convert.

Expected mapping:

| Keycloak realm role | Spring Security authority |
|---|---|
| ADMIN | ROLE_ADMIN |
| ORGANIZER | ROLE_ORGANIZER |
| ATTENDEE | ROLE_ATTENDEE |
| STAFF | ROLE_STAFF |

## 15. Important notes

1. Imports intentionally omit kiye gaye hain.
2. Ye code reference conversation mein implement kiye gaye methods/annotations ko document karta hai; complete source files ka replacement nahi hai.
3. KeycloakAdminService, registration DTOs, UserRegistrationService, global exception handler aur SecurityConfig ki complete source listings is file mein duplicate nahi ki gayi. Unka setup aur flow companion implementation-steps document mein hai.
4. Registration mein public ADMIN role selection production ke liye unsafe hai. Production mein ADMIN assignment trusted/admin-controlled process se hona chahiye.
5. STAFF role defined hai, lekin STAFF-specific workflow current implementation scope se intentionally excluded hai.
6. End-to-end tests aur final local build verification separate final activity hain.
