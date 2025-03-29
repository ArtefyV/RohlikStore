package com.rohlik.store.service;

import com.rohlik.store.exception.InsufficientStockException;
import com.rohlik.store.exception.NotFoundException;
import com.rohlik.store.model.Order;
import com.rohlik.store.model.OrderExtra;
import com.rohlik.store.model.OrderProduct;
import com.rohlik.store.model.Product;
import com.rohlik.store.repository.OrderExtraRepository;
import com.rohlik.store.repository.OrderProductRepository;
import com.rohlik.store.repository.OrderRepository;
import com.rohlik.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderExtraRepository orderExtraRepository;
    private final ProductRepository productRepository;
    private final OrderProductRepository orderProductRepository;


    /**
     * Get a list of all orders
     * @return List of all orders
     */
    public List<OrderExtra> getAllOrders() {
        log.info("Getting a list of all orders");
        return orderExtraRepository.findAll();
    }

    /**
     * Get an order by its ID
     * @param id Order ID
     * @return Order object
     */
    public OrderExtra getOrderById(Long id) {
        log.info("Getting order by ID {}", id);
        return orderExtraRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order with ID {} not found", id);
                    return new NotFoundException("Order not found");
                });
    }

    /**
     * Create a new order
     * @param order
     * @return
     */
    @Transactional
    public OrderExtra createOrder(Order order, List<OrderProduct> items) {
        log.info("Creating a new order...");
        order.prePersist();
        Order savedOrder = orderRepository.save(order);

        for (OrderProduct item : items) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> {
                        log.warn("Product with ID {} not found, order cannot be created", item.getProduct().getId());
                        return new NotFoundException("Product not found");
                    });

            if (product.getStockQuantity() < item.getQuantity()) {
                log.warn("Out of stock: {} (available {}, requested {})",
                        product.getName(), product.getStockQuantity(), item.getQuantity());
                throw new InsufficientStockException("Insufficient stock: " + product.getName());
            }

            log.info("Reserving item {} in quantity {}", product.getName(), item.getQuantity());
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);

            item.setOrder(savedOrder);
            orderProductRepository.save(item);
        }

        log.info("An order was created with ID {}", savedOrder.getId());
        return new OrderExtra(savedOrder.getId(), savedOrder.getCreatedAt(), savedOrder.isPaid(), items);
    }

/*
    public Order updateOrder(Order order){
        log.info("Updating order with ID {}", order.getId());
        Order updatedOrder = orderRepository.save(order);
        log.info("Order with ID {} has been updated", updatedOrder.getId());
        return updatedOrder;
    }
*/

    /**
     * Cancel an order
     * @param id Order ID
     */
/*
    @Transactional
    public void cancelOrder(Long id) {
        log.info("Cancel order with ID {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Attempting to cancel a non-existent order with ID {}", id);
                    return new NotFoundException("Order not found");
                });

        for (OrderProduct item : order.getItems()) {
            Product product = item.getProduct();
            log.info("Return goods {} in quantity {}", product.getName(), item.getQuantity());
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        orderRepository.deleteById(id);
        log.info("The order with ID {} has been canceled", id);
    }

*/

    /**
     * Pay for an order
     * @param id Order ID
     * @return Order object
     */
    public OrderExtra payOrder(Long id) {
        log.info("Paying for order with ID {}", id);
        OrderExtra order = orderExtraRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Attempting to pay for a non-existent order with ID {}", id);
                    return new NotFoundException("Order with ID " + id + " not found");
                });

        if (order.isPaid()) {
            throw new IllegalStateException("Order with ID " + id + " already paid");
        }

        order.setPaid(true);
        Order updatingOrder = new Order(order.getId(), order.getCreatedAt(), order.isPaid());
        orderRepository.save(updatingOrder);
        log.info("The order with ID {} has been successfully paid", id);
        return order;
    }

    /**
     * Get list of orders by paid status
     * @param paid Paid status
     *             true - paid
     *             false - not paid
     * @return List of orders
     */
    public List<OrderExtra> getOrdersByPaidStatus(boolean paid) {
        log.info("Getting a list of orders by paid status: {}", paid);
        return orderExtraRepository.findByPaid(paid);
    }
}
