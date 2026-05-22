package com.codereferee.quickbite.delivery;

import java.math.BigDecimal;
import java.time.Instant;

public final class DeliveryDtos {

    private DeliveryDtos() {
    }

    public record DeliveryView(
            Long orderId,
            String status,
            String courierName,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant updatedAt
    ) {
    }
}
