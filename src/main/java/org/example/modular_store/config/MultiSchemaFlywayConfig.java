package org.example.modular_store.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MultiSchemaFlywayConfig {

    private final DataSource dataSource;

    public MultiSchemaFlywayConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public Flyway flyway() {
        Flyway customerFlyway = migrateModule("customer_schema", "classpath:db/migration/customer");
        migrateModule("order_schema", "classpath:db/migration/order");
        return customerFlyway;
    }

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnPostProcessor() {
        return new EntityManagerFactoryDependsOnPostProcessor("flyway");
    }

    private Flyway migrateModule(String schemaName, String location) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .createSchemas(true) // Automatically executes 'CREATE SCHEMA IF NOT EXISTS' in MySQL
                .locations(location)
                .table("flyway_schema_history")
                .load();

        flyway.migrate();
        return flyway;
    }
}