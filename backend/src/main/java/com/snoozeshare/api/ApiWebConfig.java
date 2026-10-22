package com.snoozeshare.api;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers API-only handler argument resolvers when the protected API is enabled. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class ApiWebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserIdResolver());
    }
}
