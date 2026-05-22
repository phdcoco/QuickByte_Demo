package com.codereferee.quickbite.payment;

import com.codereferee.quickbite.payment.PaymentDtos.AuthorizePaymentRequest;
import com.codereferee.quickbite.payment.PaymentDtos.PaymentView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{id}/authorize")
    PaymentView authorize(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody AuthorizePaymentRequest request
    ) {
        return paymentService.authorize(id, Long.valueOf(jwt.getSubject()), request.providerReference());
    }
}
