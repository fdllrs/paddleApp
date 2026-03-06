### Phase 1: Current Feature Completion & Stabilization
- [ ] **Spatial Infrastructure Test:** Write the MockK test for `getNearbyOpenMatches` using `withArg` to strictly verify `point.x` maps to Longitude and `point.y` maps to Latitude.
- [ ] **Controller Layer Tests:** Implement `@WebMvcTest` slices to verify HTTP status codes and JSON serialization for your existing endpoints, ensuring your DTOs map correctly to the web layer.

### Phase 2: Core Domain Expansion
- [ ] **Leave Match Feature:** Implement `leaveMatch(matchId, userId)` in `MatchService` to delete the specific `MatchPlayer` junction record. You will need business logic to prevent the Host from leaving their own match without explicitly canceling it.
- [ ] **Match Lifecycle Management:** Implement `cancelMatch` or `updateMatchStatus` to transition a match from `OPEN` to `CLOSED` or `CANCELLED`.
- [ ] **Pagination:** Refactor `getNearbyOpenMatches` to use Spring Data `Pageable`. Returning unpaginated spatial queries will cause memory exhaustion on both the database and the JVM as your user base grows.

### Phase 3: Security & Architecture
- [ ] **Global Exception Handling:** Build a `@RestControllerAdvice` component. Your service layer throws `IllegalArgumentException` and `DataIntegrityViolationException`, but the frontend needs strict HTTP codes (404 Not Found, 409 Conflict). This layer bridges that gap.
- [ ] **Authentication & JWT:** Strip the `userId` from all URL parameters and request bodies. Implement Spring Security to extract the user's identity directly from a JSON Web Token in the `Authorization` header.
- [ ] **Database Migrations:** Transition away from Hibernate's `ddl-auto=update`. Implement Flyway or Liquibase to version-control your schema, especially since you are dealing with complex PostGIS extensions.