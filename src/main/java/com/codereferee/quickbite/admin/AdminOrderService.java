package com.codereferee.quickbite.admin;

import com.codereferee.quickbite.common.BusinessException;
import com.codereferee.quickbite.order.FoodOrderRepository;
import com.codereferee.quickbite.order.OrderDtos.OrderView;
import com.codereferee.quickbite.order.OrderService;
import com.codereferee.quickbite.order.OrderStatus;
import com.codereferee.quickbite.order.OrderStatusStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final FoodOrderRepository orders;
    private final OrderService orderService;
    private final OrderStatusStreamService streams;

    @Transactional
    public OrderView moveOrder(Long id, OrderStatus status) {
        var order = orders.findDetailedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
        if (order.getStatus() == OrderStatus.DELIVERED && status != OrderStatus.DELIVERED) {
            throw new BusinessException(HttpStatus.CONFLICT, "ORDER_FINALIZED", "Delivered order cannot be reopened");
        }
        order.moveTo(status);
        streams.publish(order.getId(), status, "admin-api");
        return orderService.view(order);
    }
}
