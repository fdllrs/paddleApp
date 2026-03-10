### Phase 1: Stabilization & Testing
- [x] **Spatial Infrastructure Test:** MockK test for `getNearbyOpenMatches` to strictly verify spatial coordinates.
- [x] **Leave/Cancel Logic:** Implement state transitions for `leaveMatch` and `cancelMatch`.
- [ ] **Controller Layer Tests:** Implement `@WebMvcTest` slices to verify HTTP status codes and JSON serialization for your existing endpoints.

### Phase 2: Core Architecture & Infrastructure
- [ ] **Database Migrations (High Priority):** Implement Flyway or Liquibase to manage schema changes reliably, moving away from Hibernate's unpredictable `ddl-auto=update` when handling PostGIS geometries.
- [ ] **Global Exception Handling:** Build a `@RestControllerAdvice` component to translate `IllegalArgumentException` and `SecurityException` into strict HTTP 400, 403, and 404 responses.
- [ ] **Pagination:** Refactor `getNearbyOpenMatches` to use Spring Data `Pageable` to prevent database and JVM memory exhaustion.

### Phase 3: The Deterrent System (Decision Pending)
- [ ] **Code Cleanup:** Remove the `Duration.between` time-calculation logic from `leaveMatch` and `cancelMatch` to strip out the weather/time complexity.
- [ ] **Implement Friction (Choose One):**
    - **Option A: The Strike System (Hard Limit):** Add a `cancellation_count` integer to the `User` table. Increment on cancellation. Block the user from hosting new matches for a set period if they hit 3 strikes. Reset strikes monthly via a scheduled job.
    - **Option B: The Reliability Score (Social Friction):** Add a `reliability_score` integer (0-100) to the `User` table. Add points for successfully playing, deduct heavy points for canceling/leaving. Expose this score in the `MatchResponseDTO` so the community can decide whether to join a flakey host's game.

### Phase 4: Matchmaking Engine (Decision Pending)
- [ ] **Domain Redesign (Hostless Matches):** Migrate `Match.host_id` to be nullable and introduce a `match_type` enum (`PRIVATE` vs `MATCHMAKING`).
- [ ] **The Waiting Room (Choose One):**
    - **Option A: "Create & Wait" (Lean MVP):** The `MatchmakingTicket` table strictly maps to a single user. If two friends want to play, one creates a `PRIVATE` match, the other joins, and the background engine fills the remaining 2 slots with solo players from the queue.
    - **Option B: Duo Queue / Party System (Complex):** Create a `Party` entity and a `ticket_players` junction table. The `MatchmakingTicket` holds multiple users. The background engine must calculate average party divisions and search specifically for matches with exactly 2 or 3 open slots.
- [ ] **The Background Engine:** Build a `@Scheduled` background worker that polls the ticket queue, calculates spatial, temporal, and skill overlaps, and automatically generates matches or fills open slots.

### Phase 5: Physical Reality (Court Management)
- [ ] **Court Entities:** Create a `Court` entity tied to a `Club` (e.g., "Court 1" at Obelisco Paddle) to represent physical capacity.
- [ ] **Availability Engine:** Implement database constraints (`SELECT count(*)`) to prevent the system or a host from double-booking a physical court at a specific time.

### Phase 6: Post-Match & Financials
- [ ] **Match Resolution:** Build the endpoint to update a match status from `OPEN` to `CLOSED` and record the final set scores.
- [ ] **The Financial State Machine (Mercado Pago):** Implement a two-step payment flow. **Authorize** (hold funds) when a user joins/queues. **Capture** (charge funds) when the match transitions to `CLOSED`. **Void** (cancel the hold) if the match is `CANCELLED`, avoiding refund fees.

### Phase 7: Production Readiness
- [ ] **Authentication & Security:** Strip `userId` from all URL parameters. Implement Spring Security to extract the user's identity directly from a JSON Web Token (JWT) in the `Authorization` header.
- [ ] **Elo / Division Progression:** Implement an algorithm to adjust a user's `division` (skill level) based on their win/loss ratio against specific opponent divisions.