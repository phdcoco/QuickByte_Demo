package com.codereferee.quickbite.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickbite.jwt")
public record JwtProperties(String issuer, String secret, Duration accessTokenTtl) {
}
