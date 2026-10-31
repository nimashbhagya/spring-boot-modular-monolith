package org.example.modular_store.order.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByTenantId(String tenantId);
    List<Order> findByTenantIdAndCustomerId(String tenantId, Long customerId);
    Optional<Order> findByIdAndTenantId(Long id, String tenantId);
}
