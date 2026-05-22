package com.codereferee.quickbite.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickbite.queue")
public record QueueProperties(String ordersKey, long consumerDelayMs) {
}
