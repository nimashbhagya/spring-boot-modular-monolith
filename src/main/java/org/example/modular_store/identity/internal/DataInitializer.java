package org.example.modular_store.identity.internal;

import org.example.modular_store.identity.TenantContext;
import org.example.modular_store.identity.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           TenantRepository tenantRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            // Seed Initial Tenants
            seedTenantIfMissing("system", "System Administration", "Platform super-admin root tenant");
            seedTenantIfMissing("company_abc", "Company ABC", "Primary retail store tenant");

            // 1. Seed Super Admin
            TenantContext.setTenantId("system");
            if (!userRepository.existsByEmail("superadmin@modularstore.com")) {
                AppUser superAdmin = new AppUser(
                        "superadmin@modularstore.com",
                        "System Super Admin",
                        passwordEncoder.encode("SuperAdmin@123"),
                        UserRole.SUPER_ADMIN
                );
                superAdmin.setTenantId("system");
                userRepository.save(superAdmin);
                System.out.println(">>> [SEED] Created Super Admin: superadmin@modularstore.com / SuperAdmin@123");
            }

            // 2. Seed Tenant Admin (Company ABC)
            TenantContext.setTenantId("company_abc");
            if (!userRepository.existsByEmail("admin@companyabc.com")) {
                AppUser tenantAdmin = new AppUser(
                        "admin@companyabc.com",
                        "Company ABC Admin",
                        passwordEncoder.encode("Admin@123"),
                        UserRole.TENANT_ADMIN
                );
                tenantAdmin.setTenantId("company_abc");
                userRepository.save(tenantAdmin);
                System.out.println(">>> [SEED] Created Tenant Admin: admin@companyabc.com / Admin@123");
            }

            // 3. Seed Staff User (Company ABC)
            if (!userRepository.existsByEmail("staff@companyabc.com")) {
                AppUser staff = new AppUser(
                        "staff@companyabc.com",
                        "Store Staff Member",
                        passwordEncoder.encode("Staff@123"),
                        UserRole.STAFF
                );
                staff.setTenantId("company_abc");
                userRepository.save(staff);
                System.out.println(">>> [SEED] Created Staff Member: staff@companyabc.com / Staff@123");
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void seedTenantIfMissing(String code, String name, String description) {
        if (!tenantRepository.existsByCode(code)) {
            Tenant tenant = new Tenant(code, name, description);
            tenantRepository.save(tenant);
            System.out.println(">>> [SEED] Created Tenant: " + code + " (" + name + ")");
        }
    }
}
