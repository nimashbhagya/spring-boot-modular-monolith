package org.example.modular_store.customer;

import java.util.Optional;

public interface CustomerApi {
    boolean existsById(Long customerId);
    Optional<CustomerResponse> findById(Long customerId);
}
