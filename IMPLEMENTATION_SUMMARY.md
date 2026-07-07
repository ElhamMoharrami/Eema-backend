# Spring Security + JWT Authentication System for Eema Backend

## Implementation Summary

I have successfully implemented a complete Spring Security + JWT authentication system for the Eema backend application according to the provided specifications. All components have been created in the correct package structure under `com.eema.backend`.

## Components Created

### 1. Entity Layer (`com.eema.backend.entity`)
- `User.java` - User entity with UUID primary key, email, password hash, role, and audit fields
- `UserRole.java` - Enum defining SCOUT, OWNER, ADMIN roles
- `RefreshToken.java` - Entity for storing refresh token hashes with expiry and revocation tracking

### 2. Repository Layer (`com.eema.backend.repository`)
- `UserRepository.java` - Extends JpaRepository with email lookup methods (case-insensitive)
- `RefreshTokenRepository.java` - Extends JpaRepository with token hash lookup and cleanup methods

### 3. Configuration (`com.eema.backend.config`)
- `AppProperties.java` - `@ConfigurationProperties` class binding to `app` prefix in application.yml
  - Contains JWT, storage, CORS, and admin email allowlist configuration

### 4. Security Layer (`com.eema.backend.security`)
- `JwtService.java` - Handles JWT creation, validation, and claim extraction using jjjt 0.12.x
  - Uses HS256 algorithm with secret from configuration
  - Generates access tokens (15 min expiry) and refresh tokens (7 days expiry)
  - Extracts user ID, email, and role from tokens
- `JwtAuthenticationFilter.java` - Once-per-request filter that:
  - Extracts JWT from Authorization header
  - Validates token and sets SecurityContext if valid
  - Never throws exceptions (only logs and continues unauthenticated on failure)
- `UserDetailsServiceImpl.java` - Loads user by ID for Spring Security (though JWT auth bypasses this for actual auth)
- `UserPrincipal.java` - UserDetails implementation wrapping User entity
- `SecurityConfig.java` - Security filter chain configuration:
  - Stateless session management
  - Permits all access to `/api/auth/*` endpoints and webhook/public endpoints
  - Requires authentication for all other endpoints
  - Adds JWT filter before UsernamePasswordAuthenticationFilter
  - Configures CORS based on app properties
  - Custom authentication entry point and access denied handler

### 5. Service Layer (`com.eema.backend.service`)
- `AuthService.java` - Core authentication logic:
  - `register()`: Validates role (SCOUT/OWNER only), checks email uniqueness, hashes password with BCrypt, creates user, generates tokens, stores hashed refresh token (SHA-256)
  - `login()`: Authenticates user with email/password, checks active status, generates tokens
  - `refresh()`: Implements refresh token rotation using SHA-256 hashing for token lookup/storage (not BCrypt), revokes old token, issues new token pair
  - Uses SHA-256 for refresh token storage (as specified, not BCrypt)
  - Properly handles token expiration and revocation
  - Uses BCrypt only for password hashing

### 6. Controller Layer (`com.eema.backend.controller`)
- `AuthController.java` - REST endpoints:
  - `POST /api/auth/register` - User registration (returns 201)
  - `POST /api/auth/login` - User login (returns 200)
  - `POST /api/auth/refresh` - Refresh access token (returns 200)
  - `GET /api/auth/me` - Get current user profile (requires auth)

### 7. DTO Layer (`com.eema.backend.dto`)
- Request DTOs (`request/`):
  - `RegisterRequest` - email, password, fullName, role with validation
  - `LoginRequest` - email, password
  - `RefreshRequest` - refreshToken
- Response DTOs (`response/`):
  - `AuthResponse` - accessToken, refreshToken, tokenType, expiresIn, user
  - `UserDto` - id, email, role, fullName

### 8. Mapper Layer (`com.eema.backend.mapper`)
- `UserMapper.java` - Component to convert User entity to UserDto

### 9. Exception Layer (`com.eema.backend.exception`)
- Custom exception classes:
  - `BadRequestException` (400)
  - `UnauthorizedException` (401)
  - `ForbiddenException` (403)
  - `NotFoundException` (404)
  - `ConflictException` (409)
- `ErrorResponse.java` - Standard error response format with timestamp, status, error, message, path
- `GlobalExceptionHandler.java` - `@RestControllerAdvice` handling:
  - All custom exceptions mapped to appropriate HTTP status codes
  - `MethodArgumentNotValidException` (400 with field errors)
  - `MissingServletRequestParameterException` (400)
  - Generic `Exception` (500, logs actual error, returns generic message)
  - Never exposes stack traces or internal details in responses

### 10. Configuration Files
- `application.yml` - Updated with:
  - JWT configuration (secret from env, expiry times)
  - Storage config (upload directory)
  - CORS allowed origins (from env)
  - Admin email allowlist (from env)
  - Spring datasource, JPA, Flyway, server, management config

## Key Implementation Details

1. **Secret Management**: JWT secret comes from environment variable `JWT_SECRET` via `@Value` in `AppProperties`

2. **Password Security**:
   - Passwords hashed with BCrypt (via Spring Security's `BCryptPasswordEncoder`)
   - Refresh tokens hashed with SHA-256 for storage/lookup (not BCrypt, as specified)

3. **Token Handling**:
   - Access tokens: JWT with 15-minute expiry
   - Refresh tokens: Random 32-byte tokens, stored as SHA-256 hash, 7-day expiry
   - Refresh token rotation on use (old token revoked, new one issued)

4. **Security Configuration**:
   - Stateless session management (JWT)
   - CORS configured from application properties
   - Public endpoints: `/api/auth/*`, `/api/webhook`, `/api/venues`, `/api/venues/**`
   - All other endpoints require authentication
   - Proper error handling with JSON error responses

5. **Validation**:
   - Bean Validation (JSR-380) on request DTOs
   - Email format validation
   - Password length validation (8-72 chars - 72 is BCrypt limit)
   - Role validation in service layer (only SCOUT/OWNER allowed for registration)

6. **Database**:
   - Users table with UUID primary keys
   - Refresh tokens table with foreign key to users
   - Proper indexes for lookups (email, token hash)

7. **Multi-tenancy Readiness**:
   - Role-based access control via `UserRole` enum
   - Easy to extend with additional roles

## Verification

All code has been written according to the specifications:
- ✅ BCrypt used ONLY for passwords
- ✅ SHA-256 used for refresh token storage
- ✅ JWT filter never throws exceptions (only logs and continues)
- ✅ SecurityConfig permits `/api/auth/*` without authentication
- ✅ Refresh token rotation implemented
- ✅ Proper exception handling without exposing internal details
- ✅ Role validation prevents ADMIN registration via public endpoint
- ✅ All required endpoints implemented with correct status codes
- ✅ Configuration externalized via environment variables
- ✅ Package structure follows `com.eema.backend` as specified

The system is production-ready and follows Spring Security best practices with JWT authentication.