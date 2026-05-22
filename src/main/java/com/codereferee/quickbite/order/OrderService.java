package com.codereferee.quickbite.order;

import com.codereferee.quickbite.common.BusinessException;
import com.codereferee.quickbite.order.OrderDtos.OrderItemView;
import com.codereferee.quickbite.order.OrderDtos.OrderView;
import com.codereferee.quickbite.order.OrderDtos.PlaceOrderRequest;
import com.codereferee.quickbite.order.OrderDtos.PlaceOrderResponse;
import com.codereferee.quickbite.payment.PaymentService;
import com.codereferee.quickbite.restaurant.MenuItem;
import com.codereferee.quickbite.restaurant.MenuItemRepository;
import com.codereferee.quickbite.restaurant.Restaurant;
import com.codereferee.quickbite.restaurant.RestaurantRepository;
import com.codereferee.quickbite.user.UserAccount;
import com.codereferee.quickbite.user.UserAccountRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final FoodOrderRepository orders;
    private final UserAccountRepository users;
    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final PaymentService paymentService;
    private final OrderStatusStreamService streams;

    @Transactional
    public PlaceOrderResponse place(Long customerId, PlaceOrderRequest request) {
        UserAccount customer = users.findById(customerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Authenticated user no longer exists"));
        Restaurant restaurant = restaurants.findById(request.restaurantId())
                .filter(Restaurant::isAcceptingOrders)
                .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "RESTAURANT_UNAVAILABLE", "Restaurant is not accepting orders"));

        Map<Long, MenuItem> availableItems = menuItems.findByIdInAndAvailableTrue(
                        request.items().stream().map(line -> line.menuItemId()).toList())
                .stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        FoodOrder order = new FoodOrder();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setDeliveryAddress(request.deliveryAddress().trim());
        order.setCustomerNote(request.customerNote());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (var line : request.items()) {
            MenuItem item = availableItems.get(line.menuItemId());
            if (item == null || !item.getRestaurant().getId().equals(restaurant.getId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "MENU_ITEM_UNAVAILABLE", "Order contains an unavailable menu item");
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItem(item);
            orderItem.setMenuNameSnapshot(item.getName());
            orderItem.setQuantity(line.quantity());
            orderItem.setUnitPrice(item.getPrice());
            orderItem.setLineTotal(item.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
            order.addItem(orderItem);
            subtotal = subtotal.add(orderItem.getLineTotal());
        }
        order.setSubtotal(subtotal);
        order.setDeliveryFee(restaurant.getDeliveryFee());
        order.setTotal(subtotal.add(restaurant.getDeliveryFee()));
        orders.save(order);
        var payment = paymentService.createFor(order);
        streams.publish(order.getId(), order.getStatus(), "order-api");
        return new PlaceOrderResponse(view(order), payment);
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderView> customerOrders(Long customerId) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public OrderView getForCustomer(Long customerId, Long orderId) {
        FoodOrder order = detailed(orderId);
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
        }
        return view(order);
    }

    @Transactional(readOnly = true)
    public FoodOrder detailed(Long orderId) {
        return orders.findDetailedById(orderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
    }

    public OrderView view(FoodOrder order) {
        return new OrderView(
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTotal(),
                order.getDeliveryAddress(),
                order.getCreatedAt(),
                order.getItems().stream().map(item -> new OrderItemView(
                        item.getMenuItem().getId(),
                        item.getMenuNameSnapshot(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal())).toList());
    }
}
