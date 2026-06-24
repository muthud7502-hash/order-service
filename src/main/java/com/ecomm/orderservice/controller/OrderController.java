package com.ecomm.orderservice.controller;

import com.ecomm.orderservice.client.ProductServiceClient;
import com.ecomm.orderservice.dto.OrderRequest;
import com.ecomm.orderservice.dto.ProductDto;
import com.ecomm.orderservice.model.Order;
import com.ecomm.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductServiceClient productServiceClient;

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        Optional<Order> order = orderRepository.findById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Core flow: validate product exists -> reserve stock via product-service -> save order.
    // This is the live inter-service call you'll demo in the interview.
    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        ProductDto product;
        try {
            product = productServiceClient.getProduct(request.getProductId());
        } catch (RuntimeException ex) {
            // product-service down -> order-service is up but cannot fulfil the order
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Cannot place order: product-service is unreachable.");
        }

        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Product not found: " + request.getProductId());
        }

        boolean reserved = productServiceClient.reserveStock(request.getProductId(), request.getQuantity());
        if (!reserved) {
            Order failedOrder = new Order(request.getProductId(), request.getQuantity(), 0.0, "FAILED");
            orderRepository.save(failedOrder);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Order failed: insufficient stock for product " + request.getProductId());
        }

        Double totalPrice = product.getPrice() * request.getQuantity();
        Order order = new Order(request.getProductId(), request.getQuantity(), totalPrice, "PLACED");
        Order saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
