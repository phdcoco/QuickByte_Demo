package com.codereferee.quickbite.queue;

import java.time.Instant;

public record OrderQueueMessage(Long orderId, String eventType, int attempt, Instant enqueuedAt) {
}
