package com.example.deligoandroid.Models;

import java.util.List;

public class Customization {
    private String optionId;
    private String optionName;
    private List<CustomizationSelectedItem> selectedItems;

    public Customization() {
        // Required empty constructor for Firebase
    }

    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }

    public String getOptionName() { return optionName; }
    public void setOptionName(String optionName) { this.optionName = optionName; }

    public List<CustomizationSelectedItem> getSelectedItems() { return selectedItems; }
    public void setSelectedItems(List<CustomizationSelectedItem> selectedItems) { this.selectedItems = selectedItems; }
} 