# Evently – Keycloak Authentication aur RBAC Implementation Guide

> Ye document hamari implementation conversation mein follow kiye gaye **same order** ko preserve karta hai. Isme implementation steps aur unka purpose diya gaya hai; individual prompts, debugging messages aur custom error-fixing snippets include nahi kiye gaye.

## Phase 1: Keycloak aur infrastructure setup

### Step 1 – Separate Keycloak database aur Docker setup
- Evently application database aur Keycloak database ko separate rakha:
  - Evently business/profile data: `evently-db` (local setup mein `evently` database).
  - Keycloak identity/authentication data: `keycloak-db`.
- Docker mein PostgreSQL aur Keycloak 24.0.1 configure kiya.
- Keycloak ko host port `9090` par expose kiya.
- Local development ke liye Keycloak Admin Console configure kiya.

### Step 2 – Evently realm aur roles create kiye
- Keycloak mein `Evently` naam ka realm create kiya.
- Realm-level roles create kiye:
  - `ADMIN`
  - `ORGANIZER`
  - `ATTENDEE`
  - `STAFF`
- Roles global rakhe; event-specific roles is implementation ka part nahi hain.

### Step 3 – Keycloak OIDC clients configure kiye
Do clients configure kiye:

**A. `evently-swagger` – Swagger UI login**
- Client authentication OFF (public client).
- Standard Flow ON.
- Direct Access Grants OFF.
- Swagger OAuth2 Authorization Code flow with PKCE (S256).
- Local redirect URI: `http://localhost:8080/api/v1/swagger-ui/oauth2-redirect.html`.
- Web origin: `http://localhost:8080`.

**B. `evently-service` – Backend Admin API integration**
- Client authentication ON (confidential client).
- Standard Flow OFF.
- Service Accounts Roles ON.
- Client secret ko environment variable `KEYCLOAK_CLIENT_SECRET` ke through configure kiya.
- Service account ko required `realm-management` permissions di:
  - `manage-users`
  - `view-users`
  - `query-users`
  - `view-realm`

### Step 4 – Keycloak issuer aur application properties configure ki
- Spring Boot resource server ke liye issuer URL configure ki:
  `http://localhost:9090/realms/Evently`.
- Swagger OAuth client ID, PKCE aur OAuth redirect URL configure kiye.
- Keycloak Admin API ke liye server URL, realm, client ID aur secret properties add ki.
- Client secret ko source code mein hardcode nahi kiya; local environment variable use kiya.

---

## Phase 2: Spring Security aur Swagger OAuth2

### Step 5 – Spring Security configuration implement ki
- `SecurityConfig` create/configure ki.
- Application ko stateless JWT resource server banaya.
- CSRF disable kiya (stateless API setup ke liye).
- `POST /auth/register` ko public access diya.
- Swagger/OpenAPI endpoints ko public access diya.
- Baaki application APIs ke liye authentication required rakha.
- JWT ke `realm_access.roles` claim se Keycloak realm roles extract karke Spring authorities `ROLE_<ROLE>` format mein map ki.
- Method-level authorization ke liye `@EnableMethodSecurity` enable kiya.

### Step 6 – Swagger UI OAuth2 login configure aur verify kiya
- Swagger UI ko `evently-swagger` client ke saath Authorization Code + PKCE flow par configure kiya.
- Swagger UI se Keycloak login flow verify kiya.
- Successful login ke baad Swagger UI mein authenticated session/Logout option confirm kiya.
- Duplicate OpenAPI bean configuration ko resolve karke ek hi OpenAPI configuration retain ki.

---

## Phase 3: Evently user profile aur registration integration

### Step 7 – User profile database schema update ki
- Flyway migration ke through `users_tbl` mein Keycloak mapping aur additional profile fields add kiye.
- `keycloak_user_id` column add kiya aur unique constraint rakha.
- Profile fields ko Evently DB mein maintain kiya:
  - `full_name`
  - `email`
  - `address`
  - `city`
  - `state`
  - `country`
  - `pincode`
  - `mobile_number`
  - `date_of_birth`
- `fullName` ko single field rakha; first name/last name split use nahi kiya.
- Keycloak UUID ko Evently DB mein `keycloak_user_id` ke roop mein store karna decide kiya.

### Step 8 – User entity, repository aur role enum update kiye
- `UserEntity` ko updated profile fields aur `keycloakUserId` ke saath align kiya.
- `UserRoleEnum` mein `ADMIN`, `STAFF`, `ATTENDEE`, `ORGANIZER` define kiye.
- Repository mein Keycloak UUID se user lookup aur email duplicate-check methods add kiye:
  - `findByKeycloakUserId(...)`
  - `existsByEmailIgnoreCase(...)`

### Step 9 – Registration request aur response DTOs banaye/update kiye
- Registration request DTO mein full name, email, password, role aur profile details rakhe.
- Jakarta Bean Validation annotations ke through required fields, email, password length, phone format, pincode aur DOB validations configure ki.
- Registration response DTO mein Evently user ID, Keycloak user ID, full name, email aur role return kiya.
- Password ko response DTO mein include nahi kiya.

### Step 10 – KeycloakAdminService implement ki
Backend se Keycloak Admin REST API interact karne ke liye service banayi. Iska flow:

1. `evently-service` client credentials se Keycloak access token lena.
2. Keycloak Admin API ke through user create karna.
3. Keycloak response ke `Location` header se created user ka UUID nikalna.
4. User password set karna.
5. Selected realm role ko user ke saath map karna.
6. Partial failure hone par partially created Keycloak user ko delete karke cleanup attempt karna.
7. Duplicate Keycloak username/email conflict aur integration failures ko application exceptions mein map karna.

- Keycloak username email ka `@` se pehle wala prefix rakha gaya.
- Keycloak identity record mein profile ka `fullName` field store karne ki expectation nahi rakhi; profile Evently DB mein maintain hoti hai.

### Step 11 – UserRegistrationService implement ki
Registration orchestration ka flow:

1. Evently DB mein email duplicate check.
2. KeycloakAdminService se Keycloak user create karna aur role assign karna.
3. Keycloak UUID aur submitted profile fields ke saath Evently `UserEntity` prepare karna.
4. `saveAndFlush` se Evently DB mein profile persist karna.
5. Evently DB save fail ho to Keycloak user deletion ka compensating cleanup attempt karna.
6. Successful registration par safe response DTO return karna.

### Step 12 – Registration API expose ki
- `AuthController` mein `POST /auth/register` endpoint add kiya.
- Request body par `@Valid` apply kiya.
- Successful registration par HTTP 201 Created return kiya.
- Spring Security mein registration endpoint ko public access diya.

### Step 13 – Exception handling integrate ki
- Existing `CustomGlobalExceptionHandler` ko extend kiya; duplicate global handler create nahi kiya.
- Duplicate resource/email conflict ko HTTP 409.
- Missing resource ko HTTP 404.
- DTO validation failure ko HTTP 400.
- Keycloak integration failure ko HTTP 502.
- Generic internal errors ko client-safe response ke through handle kiya.
- Internal exception details ko client response mein expose na karne ka rule rakha.

---

## Phase 4: RBAC – authenticated user identity resolution

### Step 14 – CurrentUserService create ki
- `CurrentUserService` banayi jo authenticated JWT se Keycloak user identity resolve karti hai.
- JWT ka `sub` claim (Keycloak user UUID) read kiya.
- `UserRepository.findByKeycloakUserId(...)` se corresponding Evently profile fetch ki.
- Is service ko ownership checks ke liye common identity-resolution layer banaya.

### Step 15 – Controller methods par role-level authorization lagaya
- Event create/update operations ke liye `ADMIN` aur `ORGANIZER` roles.
- Ticket purchase/cancel operations ke liye `ADMIN` aur `ATTENDEE` roles.
- Method-level checks ke liye `@PreAuthorize` use kiya.
- STAFF-specific authorization/workflow ko current scope se exclude rakha.

---

## Phase 5: Event ownership authorization

### Step 16 – Event creation ko authenticated organizer se bind kiya
- Event creation service method mein authenticated JWT pass kiya.
- `CurrentUserService` se current Evently user resolve kiya.
- Event ka `organizerUserId` request body se trust karne ke bajaye authenticated user ke Evently ID se set kiya.
- Event creation ADMIN/ORGANIZER role tak restrict ki.

### Step 17 – Event update mein ownership check add kiya
- Update service method mein JWT aur Spring `Authentication` pass kiya.
- Event fetch karke current user ka Evently ID event ke organizer ID se compare kiya.
- ADMIN ko kisi bhi event ko update karne ki permission di.
- ORGANIZER ko sirf apna event update karne diya.
- Existing event ka organizer update payload se change na ho, ye ensure kiya.

---

## Phase 6: Ticket purchase, cancellation aur purchaser ownership

### Step 18 – Ticket purchase ko authenticated attendee se bind kiya
- Purchase service method mein JWT pass kiya.
- Current Evently user ko JWT ke `sub` se resolve kiya.
- Ticket purchaser ID request body se lene ke bajaye authenticated user ID se set ki.
- Existing pessimistic stock lock, ticket availability check, purchased-ticket count aur QR code generation flow preserve kiya.
- Purchase ko ATTENDEE/ADMIN role tak restrict kiya.

### Step 19 – Ticket cancellation mein ownership check add kiya
- Cancellation service method mein JWT aur `Authentication` pass kiya.
- Current user ko ticket purchaser se compare kiya.
- ADMIN ko kisi bhi ticket ko cancel karne ki permission di.
- ATTENDEE ko sirf apna ticket cancel karne diya.
- Sirf PURCHASED status wale ticket ko cancel karne ka existing rule preserve kiya.

### Step 20 – Purchaser-wise ticket listing secure ki
- Purchaser ticket listing method mein JWT aur `Authentication` pass kiya.
- ADMIN ko kisi bhi purchaser ke tickets dekhne ki permission di.
- ATTENDEE ko sirf apne Evently user ID ke tickets dekhne diye.
- Controller se authenticated JWT aur Authentication service tak forward kiye.

---

## Phase 7: Ticket stock management authorization

### Step 21 – TicketStockService mein event ownership validation add ki
- `CurrentUserService` dependency add ki.
- Shared validation helper banaya jo:
  1. JWT se current Evently user resolve karta hai.
  2. Related event fetch karta hai.
  3. ADMIN role check karta hai.
  4. Non-admin user ke liye event organizer ownership verify karta hai.
- Ticket stock create operation mein requested event par ownership validation add ki.
- Update operation mein existing stock ke event ki ownership validate ki aur stock ko kisi doosre event mein move karne se roka.
- Delete operation mein related event ownership validate ki.
- ADMIN ko all-event ticket stock manage karne diya; ORGANIZER ko sirf apne event ka.

### Step 22 – TicketStockController create kiya
- Naya `TicketStockController` add kiya, base path `/ticket-stocks`.
- Endpoints expose kiye:
  - `POST /ticket-stocks` – create.
  - `GET /ticket-stocks` – list all.
  - `GET /ticket-stocks/{ticketStockId}` – get by ID.
  - `GET /ticket-stocks/event/{eventId}` – event-wise listing.
  - `PUT /ticket-stocks/{ticketStockId}` – update.
  - `DELETE /ticket-stocks/{ticketStockId}` – delete.
- Write operations ADMIN/ORGANIZER roles tak restrict kiye.
- Actual ownership validation service layer mein rakhi.

---

## Phase 8: Ticket read access aur status update authorization

### Step 23 – Individual ticket details secure ki
- `GET /tickets/{ticketId}` ke service method mein JWT aur Authentication pass kiya.
- ADMIN ko any ticket access diya.
- Non-admin user ko sirf apna purchased ticket dekhne diya.

### Step 24 – Sensitive ticket listing endpoints restrict kiye
- `GET /tickets/allTickets` ko ADMIN-only kiya.
- `GET /tickets/stock/{ticketStockId}` ko ADMIN-only kiya, kyunki is listing mein multiple purchasers ki ticket details expose ho sakti hain.
- Purchaser-wise listing ka owner/ADMIN check Step 20 mein implement kiya gaya tha.

### Step 25 – Manual ticket status update ADMIN-only kiya
- `PUT /tickets/{ticketId}` ko `@PreAuthorize("hasRole('ADMIN')")` se restrict kiya.
- STAFF ticket-management workflow ko current implementation scope mein include nahi kiya.

---

## Phase 9: Organizer-wise event listing authorization

### Step 26 – Organizer event listing mein ownership check add kiya
- `GET /events/organizer/{organizerUserId}` mein JWT aur Authentication pass kiya.
- ADMIN ko kisi bhi organizer ke events dekhne diye.
- ORGANIZER ko sirf apne Evently user ID ke events dekhne diye.
- Controller endpoint ko ADMIN/ORGANIZER roles tak restrict kiya.

---

## Final authorization overview

| Operation | ADMIN | ORGANIZER | ATTENDEE |
|---|---|---|---|
| Event list/detail | Allowed | Allowed | Allowed |
| Create event | Allowed | Allowed | Denied |
| Update event | Any event | Own event | Denied |
| Organizer-wise event listing | Any organizer | Own events | Denied |
| Ticket stock read endpoints | Allowed | Allowed | Allowed |
| Create/update/delete ticket stock | Any event | Own event | Denied |
| Purchase ticket | Allowed | Denied | Allowed |
| Cancel ticket | Any ticket | Own ticket* | Own ticket |
| Purchaser-wise ticket listing | Any user | Own tickets* | Own tickets |
| Individual ticket detail | Any ticket | Own ticket* | Own ticket |
| All tickets / stock-wise tickets | Allowed | Denied | Denied |
| Manual ticket status update | Allowed | Denied | Denied |

\* Current method-level role annotations for cancellation, purchaser listing and individual ticket access should be read together with service-level ownership logic; ensure the controller's final annotations match the intended roles.

## Important configuration / production notes

1. Registration mein currently chaar roles selectable hain, including `ADMIN`. Public self-registration se kisi bhi user ko ADMIN banne dena production ke liye unsafe hai. Production deployment se pehle ADMIN assignment ko trusted/admin-controlled process tak restrict karna zaroori hai.
2. Keycloak client secret ko environment variable/secret manager mein rakho; Git repository mein commit mat karo.
3. Swagger redirect URL, Keycloak issuer URL aur allowed origins ko deployment environment ke hisaab se configure karo. Localhost values production mein use nahi honi chahiye.
4. Is document mein implementation sequence record kiya gaya hai. End-to-end integration tests aur final local build/test verification ko alag final verification phase mein perform karna hai.
5. STAFF role realm mein present hai, lekin STAFF-specific feature authorization/workflow deliberately current scope se bahar rakha gaya hai.
