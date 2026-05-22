package com.codereferee.quickbite.queue;

import com.codereferee.quickbite.config.QueueProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OrderQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderQueueConsumer.class);
    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final QueueProperties queueProperties;
    private final OrderWorkflowService workflow;

    @Scheduled(fixedDelayString = "${quickbite.queue.consumer-delay-ms}")
    public void poll() {
        try {
            String payload = redis.opsForList().rightPop(queueProperties.ordersKey());
            if (payload == null) {
                return;
            }
            OrderQueueMessage message = jsonMapper.readValue(payload, OrderQueueMessage.class);
            if ("ORDER_PAID".equals(message.eventType())) {
                workflow.acceptPaidOrder(message.orderId());
                return;
            }
            log.warn("Dropping unsupported queue event type={}", message.eventType());
        } catch (RedisConnectionFailureException ex) {
            log.debug("Redis queue is unavailable: {}", ex.getMessage());
        } catch (Exception ex) {
            // The demo list queue has no DLQ yet. Production would park payloads for replay here.
            log.error("Could not process order queue payload", ex);
        }
    }
}
