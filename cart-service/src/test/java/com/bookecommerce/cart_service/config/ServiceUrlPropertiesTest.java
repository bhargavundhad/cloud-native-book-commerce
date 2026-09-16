package com.bookecommerce.cart_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

class ServiceUrlPropertiesTest {

    @Test
    void bindsServiceUrlsFromProperties() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", Map.of(
                        "app.service-urls.user", "http://localhost:8081",
                        "app.service-urls.product", "http://localhost:8082",
                        "app.service-urls.inventory", "http://localhost:8083",
                        "app.service-urls.cart", "http://localhost:8084",
                        "app.service-urls.order", "http://localhost:8085",
                        "app.service-urls.payment", "http://localhost:8086"
                ))
        );

        ServiceUrlProperties properties = Binder.get(environment)
                .bind("app.service-urls", Bindable.of(ServiceUrlProperties.class))
                .orElseThrow(() -> new IllegalStateException("Service URL configuration should bind"));

        assertThat(properties.user()).isEqualTo("http://localhost:8081");
        assertThat(properties.product()).isEqualTo("http://localhost:8082");
        assertThat(properties.inventory()).isEqualTo("http://localhost:8083");
        assertThat(properties.cart()).isEqualTo("http://localhost:8084");
        assertThat(properties.order()).isEqualTo("http://localhost:8085");
        assertThat(properties.payment()).isEqualTo("http://localhost:8086");
    }
}
