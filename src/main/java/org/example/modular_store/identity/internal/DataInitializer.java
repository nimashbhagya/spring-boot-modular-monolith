package org.example.modular_store.identity.internal;

import org.example.modular_store.identity.TenantContext;
import org.example.modular_store.identity.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            // Set context for seeding
            TenantContext.setTenantId("system");

            // 1. Seed Super Admin
            if (!userRepository.existsByEmail("superadmin@modularstore.com")) {
                AppUser superAdmin = new AppUser(
                        "superadmin@modularstore.com",
                        "System Super Admin",
                        passwordEncoder.encode("SuperAdmin@123"),
                        UserRole.SUPER_ADMIN
                );
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
                userRepository.save(staff);
                System.out.println(">>> [SEED] Created Staff Member: staff@companyabc.com / Staff@123");
            }
        } finally {
            TenantContext.clear();
        }
    }
}