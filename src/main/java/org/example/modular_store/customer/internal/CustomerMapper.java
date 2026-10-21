package org.example.modular_store.customer.internal;

import org.example.modular_store.customer.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    Customer toEntity(CustomerController.CreateCustomerRequest request);
}
