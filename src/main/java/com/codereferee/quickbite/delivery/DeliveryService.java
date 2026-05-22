package com.codereferee.quickbite.delivery;

import com.codereferee.quickbite.common.BusinessException;
import com.codereferee.quickbite.delivery.DeliveryDtos.DeliveryView;
import com.codereferee.quickbite.order.FoodOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryTrackingRepository deliveries;

    @Transactional
    public DeliveryTracking createTracking(FoodOrder order) {
        return deliveries.findByOrderId(order.getId()).orElseGet(() -> {
            DeliveryTracking tracking = new DeliveryTracking();
            tracking.setOrder(order);
            return deliveries.save(tracking);
        });
    }

    @Transactional(readOnly = true)
    public DeliveryView getForCustomer(Long orderId, Long customerId) {
        DeliveryTracking tracking = deliveries.findByOrderId(orderId)
                .filter(delivery -> delivery.getOrder().getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "Delivery tracking not found"));
        return view(tracking);
    }

    public DeliveryView view(DeliveryTracking tracking) {
        return new DeliveryView(
                tracking.getOrder().getId(),
                tracking.getStatus().name(),
                tracking.getCourierName(),
                tracking.getLatitude(),
                tracking.getLongitude(),
                tracking.getUpdatedAt());
    }
}
