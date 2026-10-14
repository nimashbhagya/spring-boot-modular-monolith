package org.example.modular_store.notification.internal;

import org.example.modular_store.customer.CustomerApi;
import org.example.modular_store.order.OrderPlacedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationListener {

    private final CustomerApi customerApi;

    public OrderNotificationListener(CustomerApi customerApi) {
        this.customerApi = customerApi;
    }

    @Async
    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        System.out.println("[NOTIFICATION THREAD: " + Thread.currentThread().getName() + "] Event received for Order #" + event.orderId());


        customerApi.findById(event.customerId()).ifPresentOrElse(customer -> {

            System.out.println("--> Notification: Email sent to " + customer.email() +
                    " for order payment of LKR " + event.amount());
        }, () -> {
            System.err.println("--> Notification Error: Customer not found!");
        });
    }
}
