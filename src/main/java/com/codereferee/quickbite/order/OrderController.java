package com.codereferee.quickbite.order;

import com.codereferee.quickbite.order.OrderDtos.OrderView;
import com.codereferee.quickbite.order.OrderDtos.PlaceOrderRequest;
import com.codereferee.quickbite.order.OrderDtos.PlaceOrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderStatusStreamService streams;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlaceOrderResponse place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.place(Long.valueOf(jwt.getSubject()), request);
    }

    @GetMapping
    List<OrderView> mine(@AuthenticationPrincipal Jwt jwt) {
        return orderService.customerOrders(Long.valueOf(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    OrderView get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.getForCustomer(Long.valueOf(jwt.getSubject()), id);
    }

    @GetMapping("/{id}/events")
    SseEmitter events(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        FoodOrder order = orderService.detailed(id);
        orderService.getForCustomer(Long.valueOf(jwt.getSubject()), id);
        return streams.subscribe(order.getId(), order.getStatus());
    }
}
