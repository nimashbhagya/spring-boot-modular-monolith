package org.example.modular_store.identity.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.example.modular_store.identity.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantRepository tenantRepository;

    public TenantController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public record TenantResponse(
            Long id,
            String code,
            String name,
            String description,
            boolean active,
            LocalDateTime createdAt
    ) {
        public static TenantResponse from(Tenant tenant) {
            return new TenantResponse(
                    tenant.getId(),
                    tenant.getCode(),
                    tenant.getName(),
                    tenant.getDescription(),
                    tenant.isActive(),
                    tenant.getCreatedAt()
            );
        }
    }

    public record CreateTenantRequest(
            @NotBlank(message = "Tenant code is required")
            String code,

            @NotBlank(message = "Tenant name is required")
            String name,

            String description
    ) {}

    public record UpdateTenantRequest(
            @NotBlank(message = "Tenant name is required")
            String name,

            String description,

            Boolean active
    ) {}

    // List tenants: SUPER_ADMIN sees all, other authenticated roles see either their tenant or active tenants
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));

        List<Tenant> tenants;
        if (isSuperAdmin) {
            tenants = tenantRepository.findAll();
        } else {
            String currentTenant = TenantContext.getTenantId();
            if (currentTenant != null && !currentTenant.isBlank()) {
                tenants = tenantRepository.findByCode(currentTenant)
                        .map(List::of)
                        .orElseGet(List::of);
            } else {
                tenants = List.of();
            }
        }

        return ResponseEntity.ok(tenants.stream().map(TenantResponse::from).toList());
    }

    // Get single tenant by code or ID
    @GetMapping("/{codeOrId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String codeOrId) {
        Tenant tenant = findTenantByCodeOrId(codeOrId);
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    // Create tenant: Super Admin only
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        String cleanCode = request.code().trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");

        if (tenantRepository.existsByCode(cleanCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant code already exists: " + cleanCode);
        }

        Tenant tenant = new Tenant(cleanCode, request.name().trim(), request.description());
        Tenant saved = tenantRepository.save(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(saved));
    }

    // Update tenant: Super Admin only
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with ID: " + id));

        tenant.setName(request.name().trim());
        if (request.description() != null) {
            tenant.setDescription(request.description());
        }
        if (request.active() != null) {
            tenant.setActive(request.active());
        }

        Tenant saved = tenantRepository.save(tenant);
        return ResponseEntity.ok(TenantResponse.from(saved));
    }

    // Delete / de-activate tenant: Super Admin only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with ID: " + id));

        if ("system".equalsIgnoreCase(tenant.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete root system tenant");
        }

        tenantRepository.delete(tenant);
        return ResponseEntity.noContent().build();
    }

    private Tenant findTenantByCodeOrId(String codeOrId) {
        try {
            Long id = Long.parseLong(codeOrId);
            return tenantRepository.findById(id)
                    .orElseGet(() -> tenantRepository.findByCode(codeOrId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found: " + codeOrId)));
        } catch (NumberFormatException e) {
            return tenantRepository.findByCode(codeOrId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found: " + codeOrId));
        }
    }
}
