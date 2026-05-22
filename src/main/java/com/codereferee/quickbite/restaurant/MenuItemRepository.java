package com.codereferee.quickbite.restaurant;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderByNameAsc(Long restaurantId);

    List<MenuItem> findByIdInAndAvailableTrue(Collection<Long> ids);
}
