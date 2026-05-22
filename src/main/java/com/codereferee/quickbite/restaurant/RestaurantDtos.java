package com.codereferee.quickbite.restaurant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class RestaurantDtos {

    private RestaurantDtos() {
    }

    public record RestaurantRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 300) String address,
            @NotNull @DecimalMin("0.00") BigDecimal deliveryFee
    ) {
    }

    public record MenuItemRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 500) String description,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            boolean available
    ) {
    }

    public record MenuItemView(Long id, String name, String description, BigDecimal price, boolean available) {
    }

    public record RestaurantView(
            Long id,
            String name,
            String address,
            BigDecimal deliveryFee,
            boolean acceptingOrders,
            List<MenuItemView> menu
    ) {
    }
}
