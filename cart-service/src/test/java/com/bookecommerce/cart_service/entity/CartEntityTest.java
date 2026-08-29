package com.bookecommerce.cart_service.entity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

class CartEntityTest {

    @Test
    void addingAndRemovingItemMaintainsBothSidesOfRelationship() {
        Cart cart = new Cart();
        CartItem item = new CartItem();

        cart.addItem(item);

        assertEquals(1, cart.getItems().size());
        assertSame(cart, item.getCart());

        cart.removeItem(item);

        assertEquals(0, cart.getItems().size());
        assertEquals(null, item.getCart());
    }

    @Test
    void newCartDefaultsToActiveStatus() {
        Cart cart = new Cart();

        assertEquals(CartStatus.ACTIVE, cart.getStatus());
        cart.setUserId(UUID.randomUUID());
    }
}
