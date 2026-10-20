package com.snoozeshare.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Exact-origin CORS for application endpoints; the actuator is configured in {@code application.yml}. */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties properties;

    /**
     * Creates the configuration.
     *
     * @param properties the validated allowed origin
     */
    public CorsConfig(CorsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(properties.allowedOrigin())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
