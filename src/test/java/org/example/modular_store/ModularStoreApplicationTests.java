package org.example.modular_store;

import org.example.modular_store.customer.internal.Customer;
import org.example.modular_store.customer.internal.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ModularStoreApplicationTests {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testSaveCustomer() {
        Customer customer = new Customer("Test User", "test_" + System.currentTimeMillis() + "@example.com");
        Customer saved = customerRepository.save(customer);
        assertNotNull(saved.getId());
        customerRepository.delete(saved);
    }

}
