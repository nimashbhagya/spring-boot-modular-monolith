package org.example.modular_store.identity.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.modular_store.identity.TenantContext;
import org.example.modular_store.identity.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record LoginRequest(
        @NotBlank(message = "Email or username is required")
        @JsonAlias({"username", "userName", "user"})
        String email,

        @NotBlank(message = "Password is required")
        String password
    ) {}

    public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotBlank String password,
        @NotNull UserRole role,
        String tenantId
    ) {}

    public record UpdateUserRequest(
        String fullName,
        UserRole role,
        Boolean active,
        String tenantId,
        String password
    ) {}

    public record UserResponse(Long id, String email, String fullName, UserRole role, boolean active, String tenantId) {
        public static UserResponse from(AppUser u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isActive(), u.getTenantId());
        }
    }

    public record AuthResponse(String token, String email, String role, String tenantId) {}

    // Public Login endpoint
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String identifier = request.email().trim();
        AppUser user = userRepository.findByEmail(identifier)
                .or(() -> {
                    if (!identifier.contains("@")) {
                        return userRepository.findByEmail(identifier + "@modularstore.com")
                                .or(() -> userRepository.findByEmail(identifier + "@companyabc.com"));
                    }
                    return java.util.Optional.empty();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getTenantId());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getTenantId()));
    }

    // Role-Guarded endpoint: List users
    // SUPER_ADMIN can see all (or filter by ?tenantId=...), TENANT_ADMIN sees only their tenant
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers(@RequestParam(required = false) String tenantId) {
        boolean isSuperAdmin = isCurrentUserSuperAdmin();
        String currentTenant = TenantContext.getTenantId();

        List<AppUser> users;
        if (isSuperAdmin) {
            if (tenantId != null && !tenantId.isBlank()) {
                users = userRepository.findByTenantId(tenantId);
            } else {
                users = userRepository.findAll();
            }
        } else {
            // TENANT_ADMIN is restricted to their current tenant
            users = userRepository.findByTenantId(currentTenant != null ? currentTenant : "default_tenant");
        }

        return ResponseEntity.ok(users.stream().map(UserResponse::from).toList());
    }

    // Role-Guarded endpoint: Register staff / admins
    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        boolean isSuperAdmin = isCurrentUserSuperAdmin();
        String currentTenant = TenantContext.getTenantId();

        // Non-super-admins cannot create SUPER_ADMIN accounts
        if (request.role() == UserRole.SUPER_ADMIN && !isSuperAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can create other SUPER_ADMIN accounts");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
        }

        // Determine target tenant
        String targetTenant;
        if (isSuperAdmin && request.tenantId() != null && !request.tenantId().isBlank()) {
            targetTenant = request.tenantId().trim();
        } else {
            targetTenant = (currentTenant != null && !currentTenant.isBlank()) ? currentTenant : "default_tenant";
        }

        AppUser newUser = new AppUser(
                request.email(),
                request.fullName(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        newUser.setTenantId(targetTenant);

        AppUser saved = userRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
    }

    // Role-Guarded endpoint: Update existing user
    @PutMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        boolean isSuperAdmin = isCurrentUserSuperAdmin();
        String currentTenant = TenantContext.getTenantId();

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + id));

        // Check tenancy boundaries for non-super-admins
        if (!isSuperAdmin) {
            if (!user.getTenantId().equals(currentTenant)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot manage users from other tenants");
            }
            if (user.getRole() == UserRole.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify a SUPER_ADMIN account");
            }
            if (request.role() == UserRole.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign SUPER_ADMIN role");
            }
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password().trim()));
        }
        if (isSuperAdmin && request.tenantId() != null && !request.tenantId().isBlank()) {
            user.setTenantId(request.tenantId().trim());
        }

        AppUser saved = userRepository.save(user);
        return ResponseEntity.ok(UserResponse.from(saved));
    }

    // Role-Guarded endpoint: Delete user
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean isSuperAdmin = isCurrentUserSuperAdmin();
        String currentTenant = TenantContext.getTenantId();

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + id));

        if (!isSuperAdmin) {
            if (!user.getTenantId().equals(currentTenant)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete users from other tenants");
            }
            if (user.getRole() == UserRole.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete a SUPER_ADMIN account");
            }
        }

        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    private boolean isCurrentUserSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));
    }
}
