package com.codereferee.quickbite.payment;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    @EntityGraph(attributePaths = {"order"})
    Optional<PaymentRequest> findDetailedById(Long id);
}
