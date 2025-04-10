package com.example.deligoandroid.Restaurant.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public class CustomizationOption implements Serializable {
    private String id;
    private String name;
    private String type;
    private boolean required;
    private int maxSelections;
    private List<CustomizationOptionItem> options;

    // Inner class for option items
    public static class CustomizationOptionItem implements Serializable {
        private String id;
        private String name;
        private double price;

        public CustomizationOptionItem() {
            // Required empty constructor for Firebase
            this.id = UUID.randomUUID().toString().toUpperCase();
        }

        public CustomizationOptionItem(String name, double price) {
            this.id = UUID.randomUUID().toString().toUpperCase();
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

        // Convert to Map for Firebase
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("name", name);
            map.put("price", price);
            return map;
        }
    }

    public CustomizationOption() {
        // Required empty constructor for Firebase
        this.id = UUID.randomUUID().toString().toUpperCase();
        this.options = new ArrayList<>();
        this.maxSelections = 1;
    }

    public CustomizationOption(String name, String type, boolean required) {
        this.id = UUID.randomUUID().toString().toUpperCase();
        this.name = name;
        this.type = type;
        this.required = required;
        this.maxSelections = 1;
        this.options = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public int getMaxSelections() { return maxSelections; }
    public void setMaxSelections(int maxSelections) { 
        this.maxSelections = maxSelections > 0 ? maxSelections : 1; 
    }

    public List<CustomizationOptionItem> getOptions() { return options; }
    public void setOptions(List<CustomizationOptionItem> options) { 
        this.options = options != null ? options : new ArrayList<>(); 
    }

    // Helper method to add an option
    public void addOption(String name, double price) {
        CustomizationOptionItem item = new CustomizationOptionItem(name, price);
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(UUID.randomUUID().toString().toUpperCase());
        }
        this.options.add(item);
    }

    // Helper method to clear all options
    public void clearOptions() {
        if (this.options == null) {
            this.options = new ArrayList<>();
        } else {
            this.options.clear();
        }
    }

    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (this.id == null || this.id.isEmpty()) {
            this.id = UUID.randomUUID().toString().toUpperCase();
        }
        map.put("id", id);
        map.put("name", name);
        map.put("type", type);
        map.put("required", required);
        map.put("maxSelections", maxSelections);
        
        List<Map<String, Object>> optionsList = new ArrayList<>();
        for (CustomizationOptionItem item : options) {
            if (item.getId() == null || item.getId().isEmpty()) {
                item.setId(UUID.randomUUID().toString().toUpperCase());
            }
            Map<String, Object> itemMap = item.toMap();
            optionsList.add(itemMap);
        }
        map.put("options", optionsList);
        
        return map;
    }

    // Create from Map (for Firebase)
    public static CustomizationOption fromMap(Map<String, Object> map) {
        CustomizationOption option = new CustomizationOption();
        option.setId((String) map.get("id"));
        option.setName((String) map.get("name"));
        option.setType((String) map.get("type"));
        option.setRequired((Boolean) map.get("required"));
        
        Object maxSelectionsObj = map.get("maxSelections");
        if (maxSelectionsObj instanceof Long) {
            option.setMaxSelections(((Long) maxSelectionsObj).intValue());
        } else if (maxSelectionsObj instanceof Integer) {
            option.setMaxSelections((Integer) maxSelectionsObj);
        }

        List<Map<String, Object>> optionsList = (List<Map<String, Object>>) map.get("options");
        if (optionsList != null) {
            List<CustomizationOptionItem> items = new ArrayList<>();
            for (Map<String, Object> itemMap : optionsList) {
                CustomizationOptionItem item = new CustomizationOptionItem();
                item.setId((String) itemMap.get("id"));
                item.setName((String) itemMap.get("name"));
                Object priceObj = itemMap.get("price");
                if (priceObj instanceof Number) {
                    item.setPrice(((Number) priceObj).doubleValue());
                }
                items.add(item);
            }
            option.setOptions(items);
        }
        
        return option;
    }
}
