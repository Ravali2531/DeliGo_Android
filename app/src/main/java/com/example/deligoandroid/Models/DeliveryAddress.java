package com.example.deligoandroid.Models;

public class DeliveryAddress {
    private String streetAddress;
    private String unit;
    private String instructions;
    private Double latitude;
    private Double longitude;

    public DeliveryAddress() {
        // Required empty constructor for Firebase
    }

    public DeliveryAddress(String streetAddress, String unit, String instructions) {
        this.streetAddress = streetAddress;
        this.unit = unit;
        this.instructions = instructions;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
} 