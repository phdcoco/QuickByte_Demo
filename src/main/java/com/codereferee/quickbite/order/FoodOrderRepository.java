package com.codereferee.quickbite.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    @EntityGraph(attributePaths = {"items", "items.menuItem", "restaurant"})
    Optional<FoodOrder> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"items", "restaurant"})
    List<FoodOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
