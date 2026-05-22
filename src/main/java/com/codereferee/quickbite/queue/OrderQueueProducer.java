package com.codereferee.quickbite.queue;

import com.codereferee.quickbite.config.QueueProperties;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OrderQueueProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderQueueProducer.class);
    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final QueueProperties queueProperties;

    public void orderPaid(Long orderId) {
        try {
            String payload = jsonMapper.writeValueAsString(new OrderQueueMessage(
                    orderId, "ORDER_PAID", 0, Instant.now()));
            redis.opsForList().leftPush(queueProperties.ordersKey(), payload);
            log.info("Queued paid order orderId={}", orderId);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize order queue message", ex);
        }
    }
}
