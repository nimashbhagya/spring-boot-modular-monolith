package org.example.modular_store.order.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "orders", schema = "order_schema")
@SuppressWarnings({"JpaDataSourceORMInspection", "unused"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId; // Plain Scalar ID (No direct Foreign Key)

    private BigDecimal totalAmount;
    private String status;

    public Order(Long customerId, BigDecimal totalAmount, String status) {
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
    }

}
