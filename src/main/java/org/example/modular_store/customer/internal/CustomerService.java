package org.example.modular_store.customer.internal;

import org.example.modular_store.identity.TenantContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Cacheable(value = "customer", key = "#id + '-' + (T(org.example.modular_store.identity.TenantContext).getTenantId() ?: 'default')")
    public Customer getCustomerById(Long id) {
        String tenantId = TenantContext.getTenantId();
        if (isCurrentUserSuperAdmin() && (tenantId == null || "system".equalsIgnoreCase(tenantId) || "ALL".equalsIgnoreCase(tenantId))) {
            return customerRepository.findById(id).orElse(null);
        }
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default_tenant";
        return customerRepository.findByIdAndTenantId(id, effectiveTenant).orElse(null);
    }

    @Cacheable(value = "customers_list", key = "T(org.example.modular_store.identity.TenantContext).getTenantId() ?: 'default'")
    public List<Customer> getAllCustomers() {
        String tenantId = TenantContext.getTenantId();
        if (isCurrentUserSuperAdmin() && (tenantId == null || "system".equalsIgnoreCase(tenantId) || "ALL".equalsIgnoreCase(tenantId))) {
            System.out.println("--> [DATABASE HIT] Fetching all customers across all tenants (SUPER_ADMIN)");
            return customerRepository.findAll();
        }
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default_tenant";
        System.out.println("--> [DATABASE HIT] Fetching customers for tenant: " + effectiveTenant);
        return customerRepository.findByTenantId(effectiveTenant);
    }

    @CacheEvict(value = "customers_list", allEntries = true)
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Caching(
        put = { @CachePut(value = "customer", key = "#id + '-' + (T(org.example.modular_store.identity.TenantContext).getTenantId() ?: 'default')") },
        evict = { @CacheEvict(value = "customers_list", allEntries = true) }
    )
    public Customer updateCustomer(Long id, String name, String email) {
        Customer customer = getCustomerById(id);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found with ID: " + id);
        }
        customer.setName(name);
        customer.setEmail(email);
        return customerRepository.save(customer);
    }

    @Caching(evict = {
        @CacheEvict(value = "customer", allEntries = true),
        @CacheEvict(value = "customers_list", allEntries = true)
    })
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found with ID: " + id);
        }
        customerRepository.delete(customer);
    }

    private boolean isCurrentUserSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));
    }
}
