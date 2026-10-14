package org.example.modular_store.customer.internal;



import org.example.modular_store.customer.CustomerApi;
import org.example.modular_store.customer.CustomerResponse;
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
        return customerRepository.existsById(customerId);
    }

    @Override
    public Optional<CustomerResponse> findById(Long customerId) {
        return customerRepository.findById(customerId)
                .map(c -> new CustomerResponse(c.getId(), c.getName(), c.getEmail()));
    }
}
