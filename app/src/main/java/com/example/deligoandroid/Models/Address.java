package com.example.deligoandroid.Models;

public class Address {
    private String street;
    private String unit;
    private String instructions;

    public Address() {
        // Required empty constructor for Firebase
    }

    public Address(String street, String unit, String instructions) {
        this.street = street;
        this.unit = unit;
        this.instructions = instructions;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
} 