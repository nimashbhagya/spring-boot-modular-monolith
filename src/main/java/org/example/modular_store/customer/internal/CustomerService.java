package org.example.modular_store.customer.internal;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
        System.out.println("--> [DATABASE HIT] Fetching Customer #" + id + " from MySQL");
        return customerRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "customers_list")
    public List<Customer> getAllCustomers() {
        System.out.println("--> [DATABASE HIT] Fetching all customers from MySQL");
        return customerRepository.findAll();
    }

    
    @CacheEvict(value = "customers_list", allEntries = true)
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Caching(
        put = { @CachePut(value = "customer", key = "#id") },
        evict = { @CacheEvict(value = "customers_list", allEntries = true) }
    )
    public Customer updateCustomer(Long id, String name, String email) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));
        customer.setName(name);
        customer.setEmail(email);
        return customerRepository.save(customer);
    }

    // Delete customer: removes both single item and list from cache
    @Caching(evict = {
        @CacheEvict(value = "customer", key = "#id"),
        @CacheEvict(value = "customers_list", allEntries = true)
    })
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }
}