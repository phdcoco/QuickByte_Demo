package com.codereferee.quickbite.payment;

import com.codereferee.quickbite.common.BusinessException;
import com.codereferee.quickbite.order.FoodOrder;
import com.codereferee.quickbite.order.OrderStatus;
import com.codereferee.quickbite.order.OrderStatusStreamService;
import com.codereferee.quickbite.payment.PaymentDtos.PaymentView;
import com.codereferee.quickbite.queue.OrderQueueProducer;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRequestRepository payments;
    private final OrderQueueProducer queueProducer;
    private final OrderStatusStreamService streams;

    @Transactional
    public PaymentView createFor(FoodOrder order) {
        PaymentRequest payment = new PaymentRequest();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        return view(payments.save(payment));
    }

    @Transactional
    public PaymentView authorize(Long paymentId, Long customerId, String providerReference) {
        PaymentRequest payment = payments.findDetailedById(paymentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment request not found"));
        FoodOrder order = payment.getOrder();
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment request not found");
        }
        if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
            return view(payment);
        }
        if (payment.getStatus() != PaymentStatus.REQUESTED || order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "PAYMENT_NOT_AUTHORIZABLE", "Payment cannot be authorized");
        }
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setProviderReference(providerReference.trim());
        payment.setAuthorizedAt(Instant.now());
        order.moveTo(OrderStatus.PAID);
        payments.save(payment);
        queueProducer.orderPaid(order.getId());
        streams.publish(order.getId(), order.getStatus(), "payment-api");
        return view(payment);
    }

    private PaymentView view(PaymentRequest payment) {
        return new PaymentView(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getAuthorizedAt());
    }
}
