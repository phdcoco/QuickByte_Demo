package com.codereferee.quickbite.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record AuthorizePaymentRequest(@NotBlank @Size(max = 160) String providerReference) {
    }

    public record PaymentView(
            Long id,
            Long orderId,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String status,
            Instant authorizedAt
    ) {
    }
}
