package com.codereferee.quickbite.admin;

import com.codereferee.quickbite.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
    }
}
