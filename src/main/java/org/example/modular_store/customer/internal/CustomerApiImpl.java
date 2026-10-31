package org.example.modular_store.customer.internal;

import org.example.modular_store.customer.CustomerApi;
import org.example.modular_store.customer.CustomerResponse;
import org.example.modular_store.identity.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
class CustomerApiImpl implements CustomerApi {

    private final CustomerRepository customerRepository;

    CustomerApiImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public boolean existsById(Long customerId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.isBlank() && !"system".equalsIgnoreCase(tenantId) && !"ALL".equalsIgnoreCase(tenantId)) {
            return customerRepository.existsByIdAndTenantId(customerId, tenantId);
        }
        return customerRepository.existsById(customerId);
    }

    @Override
    public Optional<CustomerResponse> findById(Long customerId) {
        String tenantId = TenantContext.getTenantId();
        Optional<Customer> customer;
        if (tenantId != null && !tenantId.isBlank() && !"system".equalsIgnoreCase(tenantId) && !"ALL".equalsIgnoreCase(tenantId)) {
            customer = customerRepository.findByIdAndTenantId(customerId, tenantId);
        } else {
            customer = customerRepository.findById(customerId);
        }
        return customer.map(c -> new CustomerResponse(c.getId(), c.getName(), c.getEmail()));
    }
}
