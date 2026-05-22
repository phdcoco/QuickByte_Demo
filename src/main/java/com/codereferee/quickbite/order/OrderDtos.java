package com.codereferee.quickbite.order;

import com.codereferee.quickbite.payment.PaymentDtos.PaymentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(
            @NotNull Long restaurantId,
            @NotBlank @Size(max = 300) String deliveryAddress,
            @Size(max = 300) String customerNote,
            @NotEmpty List<@Valid OrderLineRequest> items
    ) {
    }

    public record OrderLineRequest(@NotNull Long menuItemId, @Min(1) int quantity) {
    }

    public record OrderItemView(
            Long menuItemId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }

    public record OrderView(
            Long id,
            Long restaurantId,
            String restaurantName,
            String status,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal total,
            String deliveryAddress,
            Instant createdAt,
            List<OrderItemView> items
    ) {
    }

    public record PlaceOrderResponse(OrderView order, PaymentView payment) {
    }

    public record OrderStatusEvent(Long orderId, String status, Instant changedAt, String source) {
    }
}
