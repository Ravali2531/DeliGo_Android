package com.example.deligoandroid.Restaurant.Models;

import java.util.ArrayList;
import java.util.List;

public class CustomizationOption {
    private String id;
    private String name;
    private String type; // "single" or "multiple"
    private boolean required;
    private int maxSelections;
    private List<String> options;

    public CustomizationOption() {
        // Required empty constructor for Firebase
        this.options = new ArrayList<>();
        this.maxSelections = 0;
    }

    public CustomizationOption(String name, String type, boolean required) {
        this();  // Call the default constructor
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public int getMaxSelections() {
        return maxSelections;
    }

    public void setMaxSelections(int maxSelections) {
        this.maxSelections = maxSelections;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public void addOption(String option) {
        if (options == null) {
            options = new ArrayList<>();
        }
        options.add(option);
    }
}
//
//package com.example.deligoandroid.Restaurant.Models;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class CustomizationOption {
//    private String name;
//    private String type; // "single" or "multiple"
//    private boolean required;
//    private int maxSelections;
//    private List<CustomizationItem> options;
//
//    public CustomizationOption() {
//        // Required empty constructor for Firebase
//        this.options = new ArrayList<>();
//        this.maxSelections = 0;
//    }
//
//    public CustomizationOption(String name, String type, boolean required, int maxSelections) {
//        this.name = name;
//        this.type = type;
//        this.required = required;
//        this.maxSelections = maxSelections;
//        this.options = new ArrayList<>();
//    }
//
//    // Getters and Setters
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public String getType() { return type; }
//    public void setType(String type) { this.type = type; }
//
//    public boolean isRequired() { return required; }
//    public void setRequired(boolean required) { this.required = required; }
//
//    public int getMaxSelections() { return maxSelections; }
//    public void setMaxSelections(int maxSelections) { this.maxSelections = maxSelections; }
//
//    public List<CustomizationItem> getOptions() { return options; }
//    public void setOptions(List<CustomizationItem> options) { this.options = options; }
//
//    public void addOption(CustomizationItem option) {
//        if (options == null) {
//            options = new ArrayList<>();
//        }
//        options.add(option);
//    }
//}
