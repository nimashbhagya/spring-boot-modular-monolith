package org.example.modular_store.customer.internal;

import org.example.modular_store.customer.CustomerResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    
    CustomerResponse toResponse(Customer customer);

    Customer toEntity(CustomerController.CreateCustomerRequest request);
}
