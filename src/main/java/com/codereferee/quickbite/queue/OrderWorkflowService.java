package com.codereferee.quickbite.queue;

import com.codereferee.quickbite.delivery.DeliveryService;
import com.codereferee.quickbite.order.FoodOrderRepository;
import com.codereferee.quickbite.order.OrderStatus;
import com.codereferee.quickbite.order.OrderStatusStreamService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(OrderWorkflowService.class);
    private final FoodOrderRepository orders;
    private final DeliveryService deliveryService;
    private final OrderStatusStreamService streams;

    @Transactional
    public void acceptPaidOrder(Long orderId) {
        orders.findById(orderId).ifPresentOrElse(order -> {
            if (order.getStatus() != OrderStatus.PAID) {
                log.info("Skipping async workflow for orderId={} status={}", orderId, order.getStatus());
                return;
            }
            order.moveTo(OrderStatus.RESTAURANT_CONFIRMED);
            deliveryService.createTracking(order);
            streams.publish(order.getId(), order.getStatus(), "redis-consumer");
        }, () -> log.warn("Queue referenced missing orderId={}", orderId));
    }
}
