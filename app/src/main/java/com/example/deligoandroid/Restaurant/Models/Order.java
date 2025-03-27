package com.example.deligoandroid.Restaurant.Models;

import com.google.firebase.database.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private String id;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String restaurantId;
    private String status; // PENDING, ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED, CANCELLED
    private String orderStatus;
    private double totalAmount;
    private String deliveryAddress;
    private long timestamp;
    private List<OrderItem> items;
    private String driverId;
    private String driverName;
    private double deliveryFee;
    private String deliveryOption;

    public Order() {
        // Initialize with default values
        this.items = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.totalAmount = 0.0;
        this.deliveryFee = 0.0;
        this.status = "NEW";
        this.customerPhone = "";
    }

    @PropertyName("id")
    public String getId() { return id != null ? id : ""; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("customerId")
    public String getCustomerId() { return customerId != null ? customerId : ""; }
    @PropertyName("customerId")
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    @PropertyName("customerName")
    public String getCustomerName() { return customerName != null ? customerName : ""; }
    @PropertyName("customerName")
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    @PropertyName("customerPhone")
    public String getCustomerPhone() { return customerPhone != null ? customerPhone : ""; }
    @PropertyName("customerPhone")
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    @PropertyName("restaurantId")
    public String getRestaurantId() { return restaurantId != null ? restaurantId : ""; }
    @PropertyName("restaurantId")
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    @PropertyName("status")
    public String getStatus() { return status != null ? status : "NEW"; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("orderStatus")
    public String getOrderStatus() { return orderStatus != null ? orderStatus : ""; }
    @PropertyName("orderStatus")
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    @PropertyName("totalAmount")
    public double getTotalAmount() { return totalAmount; }
    @PropertyName("totalAmount")
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    @PropertyName("deliveryAddress")
    public String getDeliveryAddress() { return deliveryAddress != null ? deliveryAddress : ""; }
    @PropertyName("deliveryAddress")
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    @PropertyName("timestamp")
    public long getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) { this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis(); }

    @PropertyName("items")
    public List<OrderItem> getItems() { return items != null ? items : new ArrayList<>(); }
    @PropertyName("items")
    public void setItems(List<OrderItem> items) { this.items = items != null ? items : new ArrayList<>(); }

    @PropertyName("driverId")
    public String getDriverId() { return driverId != null ? driverId : ""; }
    @PropertyName("driverId")
    public void setDriverId(String driverId) { this.driverId = driverId; }

    @PropertyName("driverName")
    public String getDriverName() { return driverName != null ? driverName : ""; }
    @PropertyName("driverName")
    public void setDriverName(String driverName) { this.driverName = driverName; }

    @PropertyName("deliveryFee")
    public double getDeliveryFee() { return deliveryFee; }
    @PropertyName("deliveryFee")
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    @PropertyName("deliveryOption")
    public String getDeliveryOption() { return deliveryOption != null ? deliveryOption : ""; }
    @PropertyName("deliveryOption")
    public void setDeliveryOption(String deliveryOption) { this.deliveryOption = deliveryOption; }
} 