package com.example.deligoandroid.Customer.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomizationSelection implements Serializable {
    private String id;
    private String name;
    private double price;
    private String optionId;
    private String optionName;
    private List<SelectedItem> selectedItems;

    public CustomizationSelection() {
        this.selectedItems = new ArrayList<>();
    }

    public CustomizationSelection(CustomizationItem item) {
        this.id = item.getId();
        this.name = item.getName();
        this.price = item.getPrice();
        this.selectedItems = new ArrayList<>();
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getOptionId() {
        return optionId;
    }

    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public List<SelectedItem> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<SelectedItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CustomizationSelection that = (CustomizationSelection) o;

        if (Double.compare(that.price, price) != 0) return false;
        if (!id.equals(that.id)) return false;
        if (!name.equals(that.name)) return false;
        if (!optionId.equals(that.optionId)) return false;
        if (!optionName.equals(that.optionName)) return false;
        return selectedItems.equals(that.selectedItems);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id.hashCode();
        result = 31 * result + name.hashCode();
        temp = Double.doubleToLongBits(price);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + optionId.hashCode();
        result = 31 * result + optionName.hashCode();
        result = 31 * result + selectedItems.hashCode();
        return result;
    }
} 