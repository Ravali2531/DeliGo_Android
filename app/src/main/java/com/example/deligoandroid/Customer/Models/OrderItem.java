package com.example.deligoandroid.Customer.Models;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class OrderItem {
    private String itemId;
    private String name;
    private int quantity;
    private double price;
    private String specialInstructions;
    private List<CustomizationOption> customizations;

    public OrderItem() {
        customizations = new ArrayList<>();
    }

    // Static nested classes for customizations
    public static class CustomizationOption {
        private String optionId;
        private String optionName;
        private List<SelectedItem> selectedItems;

        public CustomizationOption() {
            selectedItems = new ArrayList<>();
        }

        public String getOptionId() { return optionId; }
        public void setOptionId(String optionId) { this.optionId = optionId; }

        public String getOptionName() { return optionName; }
        public void setOptionName(String optionName) { this.optionName = optionName; }

        public List<SelectedItem> getSelectedItems() { return selectedItems; }
        public void setSelectedItems(List<SelectedItem> selectedItems) { this.selectedItems = selectedItems; }
    }

    public static class SelectedItem {
        private String id;
        private String name;
        private double price;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
    }

    // Getters and setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public List<CustomizationOption> getCustomizations() { return customizations; }
    public void setCustomizations(List<CustomizationOption> customizations) { this.customizations = customizations; }

    // Helper method to convert from Firebase Map
    public static OrderItem fromMap(Map<String, Object> map, Map<String, Object> customizationsMap) {
        OrderItem item = new OrderItem();
        
        item.setItemId((String) map.get("itemId"));
        item.setName((String) map.get("name"));
        item.setQuantity(map.get("quantity") instanceof Long ? ((Long) map.get("quantity")).intValue() : 1);
        
        Object priceObj = map.get("price");
        if (priceObj instanceof Double) {
            item.setPrice((Double) priceObj);
        } else if (priceObj instanceof Long) {
            item.setPrice(((Long) priceObj).doubleValue());
        }
        
        item.setSpecialInstructions((String) map.get("specialInstructions"));

        // Handle customizations
        if (customizationsMap != null) {
            for (Map.Entry<String, Object> entry : customizationsMap.entrySet()) {
                String customizationId = entry.getKey();
                Object customizationObj = entry.getValue();
                
                if (customizationObj instanceof Map) {
                    Map<String, Object> customizationMap = (Map<String, Object>) customizationObj;
                    
                    CustomizationOption option = new CustomizationOption();
                    option.setOptionId(customizationId);
                    option.setOptionName((String) customizationMap.get("optionName"));
                    
                    Object selectedItemsObj = customizationMap.get("selectedItems");
                    if (selectedItemsObj instanceof List) {
                        List<Map<String, Object>> selectedItemsData = (List<Map<String, Object>>) selectedItemsObj;
                        
                        for (Map<String, Object> selectedItemMap : selectedItemsData) {
                            SelectedItem selectedItem = new SelectedItem();
                            selectedItem.setId((String) selectedItemMap.get("id"));
                            selectedItem.setName((String) selectedItemMap.get("name"));
                            
                            Object selectedItemPrice = selectedItemMap.get("price");
                            if (selectedItemPrice instanceof Double) {
                                selectedItem.setPrice((Double) selectedItemPrice);
                            } else if (selectedItemPrice instanceof Long) {
                                selectedItem.setPrice(((Long) selectedItemPrice).doubleValue());
                            }
                            
                            option.getSelectedItems().add(selectedItem);
                        }
                    }
                    
                    item.getCustomizations().add(option);
                }
            }
        }
        
        return item;
    }
} 