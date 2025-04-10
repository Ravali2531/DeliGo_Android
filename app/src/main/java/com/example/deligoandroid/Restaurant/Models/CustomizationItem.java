package com.example.deligoandroid.Restaurant.Models;

public class CustomizationItem {
    private String id;
    private String name;
    private double price;

    public CustomizationItem() {
        // Required empty constructor for Firebase
    }

    public CustomizationItem(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
//
//package com.example.deligoandroid.Restaurant.Models;
//
//public class CustomizationItem {
//    private String id;
//    private String name;
//    private double price;
//    private boolean required;
//    private String type;
//
//    public CustomizationItem() {
//        // Required empty constructor for Firebase
//    }
//
//    public CustomizationItem(String id, String name, double price, boolean required, String type) {
//        this.id = id;
//        this.name = name;
//        this.price = price;
//        this.required = required;
//        this.type = type;
//    }
//
//    // Getters and Setters
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public double getPrice() { return price; }
//    public void setPrice(double price) { this.price = price; }
//
//    public boolean isRequired() { return required; }
//    public void setRequired(boolean required) { this.required = required; }
//
//    public String getType() { return type; }
//    public void setType(String type) { this.type = type; }
//}
