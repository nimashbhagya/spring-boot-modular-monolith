package org.example.modular_store.customer.internal;

import jakarta.persistence.*;
import lombok.Getter;
import java.io.Serializable;

@Getter
@Entity
@Table(name = "customers", schema = "customer_schema")
@SuppressWarnings({"JpaDataSourceORMInspection", "unused"})
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    protected Customer() {
    }

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

}
