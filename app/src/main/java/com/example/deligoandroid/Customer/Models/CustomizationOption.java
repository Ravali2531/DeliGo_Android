package com.example.deligoandroid.Customer.Models;

import java.io.Serializable;
import java.util.List;

public class CustomizationOption implements Serializable {
    private String id;
    private String name;
    private String type; // "single" or "multiple"
    private boolean required;
    private List<CustomizationItem> options;
    private int maxSelections;

    public CustomizationOption() {
        // Required empty constructor for Firebase
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

    public List<CustomizationItem> getOptions() { return options; }
    public void setOptions(List<CustomizationItem> options) { this.options = options; }

    public int getMaxSelections() { return maxSelections; }
    public void setMaxSelections(int maxSelections) { this.maxSelections = maxSelections; }

    public boolean isSingleSelection() {
        return "single".equals(type);
    }
} 