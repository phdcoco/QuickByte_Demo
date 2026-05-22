package com.codereferee.quickbite.delivery;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, Long> {

    @EntityGraph(attributePaths = {"order"})
    Optional<DeliveryTracking> findByOrderId(Long orderId);
}
