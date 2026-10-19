package org.example.modular_store.customer.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.example.modular_store.customer.CustomerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public CustomerController(CustomerService customerService, CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    public record CreateCustomerRequest(
        @NotBlank(message = "Name cannot be empty")
        String name,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Invalid email format")
        String email
    ) {}

    public record UpdateCustomerRequest(
        @NotBlank(message = "Name cannot be empty")
        String name,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Invalid email format")
        String email
    ) {}

    // 1. Create Customer 
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerMapper.toEntity(request);
        Customer savedCustomer = customerService.createCustomer(customer);
        return ResponseEntity.ok(customerMapper.toResponse(savedCustomer));
    }

    // 2. Get All Customers (Redis Cached)
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerResponse> response = customers.stream()
                .map(customerMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    // 3. Get Single Customer by ID (Redis Cached)
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customerMapper.toResponse(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Customer updated = customerService.updateCustomer(id, request.name(), request.email());
        return ResponseEntity.ok(customerMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}