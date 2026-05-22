package com.codereferee.quickbite.restaurant;

import com.codereferee.quickbite.restaurant.RestaurantDtos.RestaurantView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping
    List<RestaurantView> list() {
        return restaurantService.list();
    }

    @GetMapping("/{id}")
    RestaurantView get(@PathVariable Long id) {
        return restaurantService.get(id);
    }
}
