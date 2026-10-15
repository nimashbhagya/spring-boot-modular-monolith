package org.example.modular_store.order.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.modular_store.customer.CustomerApi;
import org.springframework.web.server.ResponseStatusException;
import org.example.modular_store.order.OrderPlacedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final CustomerApi customerApi;
    private final ApplicationEventPublisher eventPublisher;

    public OrderController(OrderRepository orderRepository,
                           CustomerApi customerApi,
                           ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.customerApi = customerApi;
        this.eventPublisher = eventPublisher;
    }

    public record CreateOrderRequest(Long customerId, BigDecimal amount) {}

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        if (!customerApi.existsById(request.customerId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer not found with ID: " + request.customerId()
            );
        }
       
        Order order = new Order(request.customerId(), request.amount(), "COMPLETED");
        Order savedOrder = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPlacedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmount()
        ));

        return ResponseEntity.ok(savedOrder);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam(required = false) Long customerId) {
    if (customerId != null) {
        return ResponseEntity.ok(orderRepository.findByCustomerId(customerId));
    }
    return ResponseEntity.ok(orderRepository.findAll());
}
}
