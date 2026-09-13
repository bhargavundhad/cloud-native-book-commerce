package com.bookecommerce.cart_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.service-urls")
public record ServiceUrlProperties(
        String user,
        String product,
        String inventory,
        String cart,
        String order,
        String payment
) {
}
