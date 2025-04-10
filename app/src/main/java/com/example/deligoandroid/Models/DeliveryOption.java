package com.example.deligoandroid.Models;

public enum DeliveryOption {
    DELIVERY("Delivery"),
    PICKUP("Pickup");

    private final String value;

    DeliveryOption(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 