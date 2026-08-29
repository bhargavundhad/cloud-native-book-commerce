package com.bookecommerce.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    @Column(name = "shipping_address", nullable = false)
    private String address;

    public static ShippingAddress of(String address) {
        return new ShippingAddress(address);
    }
}
