package com.codereferee.quickbite.delivery;

import com.codereferee.quickbite.delivery.DeliveryDtos.DeliveryView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/orders/{orderId}")
    DeliveryView get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
        return deliveryService.getForCustomer(orderId, Long.valueOf(jwt.getSubject()));
    }
}
