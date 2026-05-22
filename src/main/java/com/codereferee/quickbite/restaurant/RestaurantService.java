package com.codereferee.quickbite.restaurant;

import com.codereferee.quickbite.common.BusinessException;
import com.codereferee.quickbite.restaurant.RestaurantDtos.MenuItemRequest;
import com.codereferee.quickbite.restaurant.RestaurantDtos.MenuItemView;
import com.codereferee.quickbite.restaurant.RestaurantDtos.RestaurantRequest;
import com.codereferee.quickbite.restaurant.RestaurantDtos.RestaurantView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;

    @Transactional(readOnly = true)
    public List<RestaurantView> list() {
        return restaurants.findAll().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public RestaurantView get(Long id) {
        return view(findRestaurant(id));
    }

    @Transactional
    public RestaurantView create(RestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.name().trim());
        restaurant.setAddress(request.address().trim());
        restaurant.setDeliveryFee(request.deliveryFee());
        return view(restaurants.save(restaurant));
    }

    @Transactional
    public MenuItemView addMenuItem(Long restaurantId, MenuItemRequest request) {
        MenuItem item = new MenuItem();
        item.setRestaurant(findRestaurant(restaurantId));
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setAvailable(request.available());
        return menuView(menuItems.save(item));
    }

    private RestaurantView view(Restaurant restaurant) {
        return new RestaurantView(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.getDeliveryFee(),
                restaurant.isAcceptingOrders(),
                menuItems.findByRestaurantIdAndAvailableTrueOrderByNameAsc(restaurant.getId()).stream()
                        .map(this::menuView)
                        .toList());
    }

    private MenuItemView menuView(MenuItem item) {
        return new MenuItemView(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.isAvailable());
    }

    private Restaurant findRestaurant(Long id) {
        return restaurants.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RESTAURANT_NOT_FOUND", "Restaurant not found"));
    }
}
