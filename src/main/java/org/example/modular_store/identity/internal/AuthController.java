package org.example.modular_store.identity.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.modular_store.identity.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
        @NotNull UserRole role
    ) {}

    public record UserResponse(Long id, String email, String fullName, UserRole role, String tenantId) {}
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

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getTenantId()));
    }

    // Role-Guarded endpoint: Only Super Admin and Tenant Admin can register staff / admins
    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
        }

        AppUser newUser = new AppUser(
                request.email(),
                request.fullName(),
                passwordEncoder.encode(request.password()),
                request.role()
        );

        AppUser saved = userRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new UserResponse(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getRole(), saved.getTenantId())
        );
    }

    // Role-Guarded: List all users under current context
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getTenantId()))
                .toList();
        return ResponseEntity.ok(users);
    }
}