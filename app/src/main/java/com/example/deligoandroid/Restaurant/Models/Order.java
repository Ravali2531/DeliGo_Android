package com.example.deligoandroid.Restaurant.Models;

import java.util.List;
import java.util.Map;

public class Order {
    private String id;
    private String customerId;
    private String customerName;
    private String status; // PENDING, ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED, CANCELLED
    private double totalAmount;
    private String deliveryAddress;
    private long timestamp;
    private Map<String, OrderItem> items;
    private String driverId;
    private String driverName;

    public Order() {} // Required for Firebase

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Map<String, OrderItem> getItems() { return items; }
    public void setItems(Map<String, OrderItem> items) { this.items = items; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
} 