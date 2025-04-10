package com.example.deligoandroid.Models;

public enum PaymentMethod {
    CARD("Credit/Debit Card"),
    COD("Cash on Delivery");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 