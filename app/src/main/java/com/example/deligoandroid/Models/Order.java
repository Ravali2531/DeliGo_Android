package com.example.deligoandroid.Models;

import java.util.List;
import java.util.Map;
import android.util.Log;
import java.util.HashMap;

public class Order {
    private String orderId;
    private String customerId;
    private String customerName;
    private String restaurantId;
    private String restaurantName;
    private List<Map<String, Object>> items;
    private double totalAmount;
    private String status;
    private String orderStatus;
    private String driverId;
    private String driverName;
    private boolean driverAccepted;
    private String deliveryOption;
    private String deliveryAddress;
    private double deliveryLatitude;
    private double deliveryLongitude;
    private long timestamp;
    private String id;
    private Double deliveryFee;
    private Object total;  // Added for Firebase total field
    private Map<String, Object> customizations;  // Updated type to match Firebase structure
    private Address address;

    public Order() {
        // Required empty constructor for Firebase
    }

    // Getters and Setters
    public String getOrderId() {
        // If orderId is null, fallback to id
        return orderId != null ? orderId : id;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
        // Also set id if it's not already set
        if (this.id == null) {
            this.id = orderId;
        }
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public boolean isDriverAccepted() { return driverAccepted; }
    public void setDriverAccepted(boolean driverAccepted) { this.driverAccepted = driverAccepted; }

    public String getDeliveryOption() { return deliveryOption; }
    public void setDeliveryOption(String deliveryOption) { this.deliveryOption = deliveryOption; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public double getDeliveryLatitude() { return deliveryLatitude; }
    public void setDeliveryLatitude(double deliveryLatitude) { this.deliveryLatitude = deliveryLatitude; }

    public double getDeliveryLongitude() { return deliveryLongitude; }
    public void setDeliveryLongitude(double deliveryLongitude) { this.deliveryLongitude = deliveryLongitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getId() {
        // Log the ID value for debugging
        Log.d("Order", "Getting ID: " + id);
        return id;
    }

    public void setId(String id) {
        // Log the ID being set
        Log.d("Order", "Setting ID: " + id);
        this.id = id;
        // Also set orderId for compatibility
        this.orderId = id;
    }

    public Map<String, Object> getCustomizations() { return customizations; }
    public void setCustomizations(Map<String, Object> customizations) { this.customizations = customizations; }

    public Object getTotal() { return total; }
    public void setTotal(Object total) { this.total = total; }

    public Double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(Double deliveryFee) { this.deliveryFee = deliveryFee; }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", getOrderId());
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("restaurantId", restaurantId);
        map.put("restaurantName", restaurantName);
        map.put("items", items);
        map.put("total", total);
        map.put("status", status);
        map.put("orderStatus", orderStatus);
        map.put("driverId", driverId);
        map.put("driverName", driverName);
        map.put("driverAccepted", driverAccepted);
        map.put("deliveryOption", deliveryOption);
        map.put("deliveryAddress", deliveryAddress);
        map.put("deliveryLatitude", deliveryLatitude);
        map.put("deliveryLongitude", deliveryLongitude);
        map.put("timestamp", timestamp);
        map.put("deliveryFee", deliveryFee);
        map.put("customizations", customizations);
        return map;
    }
} 