package com.codereferee.quickbite.admin;

import com.codereferee.quickbite.admin.AdminDtos.UpdateOrderStatusRequest;
import com.codereferee.quickbite.order.OrderDtos.OrderView;
import com.codereferee.quickbite.restaurant.RestaurantDtos.MenuItemRequest;
import com.codereferee.quickbite.restaurant.RestaurantDtos.MenuItemView;
import com.codereferee.quickbite.restaurant.RestaurantDtos.RestaurantRequest;
import com.codereferee.quickbite.restaurant.RestaurantDtos.RestaurantView;
import com.codereferee.quickbite.restaurant.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RestaurantService restaurantService;
    private final AdminOrderService adminOrderService;

    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    RestaurantView createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        return restaurantService.create(request);
    }

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    MenuItemView addMenu(@PathVariable Long restaurantId, @Valid @RequestBody MenuItemRequest request) {
        return restaurantService.addMenuItem(restaurantId, request);
    }

    @PatchMapping("/orders/{id}/status")
    OrderView updateOrder(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return adminOrderService.moveOrder(id, request.status());
    }
}
