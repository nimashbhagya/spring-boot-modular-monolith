package org.example.modular_store.customer.internal;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    
    @Cacheable(value = "customer", key = "#id")
    public Customer getCustomerById(Long id) {
        System.out.println("--> [DATABASE HIT] Fetching Customer #" + id + " from PostgreSQL");
        return customerRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "customers_list")
    public List<Customer> getAllCustomers() {
        System.out.println("--> [DATABASE HIT] Fetching all customers from PostgreSQL");
        return customerRepository.findAll();
    }

    
    @CacheEvict(value = "customers_list", allEntries = true)
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
}