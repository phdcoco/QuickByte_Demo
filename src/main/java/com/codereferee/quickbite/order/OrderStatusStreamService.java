package com.codereferee.quickbite.order;

import com.codereferee.quickbite.order.OrderDtos.OrderStatusEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class OrderStatusStreamService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusStreamService.class);
    private final Map<Long, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long orderId, OrderStatus currentStatus) {
        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L);
        subscribers.computeIfAbsent(orderId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(orderId, emitter));
        emitter.onTimeout(() -> remove(orderId, emitter));
        send(emitter, new OrderStatusEvent(orderId, currentStatus.name(), Instant.now(), "snapshot"));
        return emitter;
    }

    @Async
    public void publish(Long orderId, OrderStatus status, String source) {
        OrderStatusEvent event = new OrderStatusEvent(orderId, status.name(), Instant.now(), source);
        subscribers.getOrDefault(orderId, List.of()).forEach(emitter -> send(emitter, event));
    }

    private void send(SseEmitter emitter, OrderStatusEvent event) {
        try {
            emitter.send(SseEmitter.event().name("order-status").data(event));
        } catch (IOException ex) {
            log.debug("Dropping stale order status subscriber", ex);
            emitter.complete();
        }
    }

    private void remove(Long orderId, SseEmitter emitter) {
        subscribers.computeIfPresent(orderId, (ignored, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
