package com.snoozeshare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * The single browser origin allowed to call the backend.
 *
 * @param allowedOrigin one http(s) origin without wildcard or trailing slash
 */
@Validated
@ConfigurationProperties(prefix = "snoozeshare.cors")
public record CorsProperties(
    @NotBlank
    @Pattern(regexp = "^https?://[^/*\s]+$",
        message = "snoozeshare.cors.allowed-origin must be one http(s) origin without wildcard or trailing slash")
    String allowedOrigin) { }
