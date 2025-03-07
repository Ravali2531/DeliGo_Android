package com.example.deligoandroid.Customer.Models;

import java.io.Serializable;
import androidx.annotation.NonNull;

public class Restaurant implements Serializable {
    private String id;
    private String name;
    private String description;
    private String email;
    private String phone;
    private String cuisine;
    private String priceRange;
    private double rating;
    private int numberOfRatings;
    private String address;
    private String imageURL;
    private boolean isOpen;

    public Restaurant() {
        // Required empty constructor for Firebase
        this.id = "";
        this.name = "";
        this.description = "";
        this.email = "";
        this.phone = "";
        this.cuisine = "";
        this.priceRange = "";
        this.rating = 0.0;
        this.numberOfRatings = 0;
        this.address = "";
        this.imageURL = "";
        this.isOpen = false;
    }

    // Getters and Setters with null checks
    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id != null ? id : ""; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name != null ? name : ""; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }

    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email != null ? email : ""; }

    public String getPhone() { return phone != null ? phone : ""; }
    public void setPhone(String phone) { this.phone = phone != null ? phone : ""; }

    public String getCuisine() { return cuisine != null ? cuisine : ""; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine != null ? cuisine : ""; }

    public String getPriceRange() { return priceRange != null ? priceRange : ""; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange != null ? priceRange : ""; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getNumberOfRatings() { return numberOfRatings; }
    public void setNumberOfRatings(int numberOfRatings) { this.numberOfRatings = numberOfRatings; }

    public String getAddress() { return address != null ? address : ""; }
    public void setAddress(String address) { this.address = address != null ? address : ""; }

    public String getImageURL() { return imageURL; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }

    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Restaurant that = (Restaurant) o;

        if (Double.compare(that.rating, rating) != 0) return false;
        if (numberOfRatings != that.numberOfRatings) return false;
        if (isOpen != that.isOpen) return false;
        if (!getId().equals(that.getId())) return false;
        if (!getName().equals(that.getName())) return false;
        if (!getDescription().equals(that.getDescription())) return false;
        if (!getEmail().equals(that.getEmail())) return false;
        if (!getPhone().equals(that.getPhone())) return false;
        if (!getCuisine().equals(that.getCuisine())) return false;
        if (!getPriceRange().equals(that.getPriceRange())) return false;
        if (!getAddress().equals(that.getAddress())) return false;
        return getImageURL().equals(that.getImageURL());
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = getId().hashCode();
        result = 31 * result + getName().hashCode();
        result = 31 * result + getDescription().hashCode();
        result = 31 * result + getEmail().hashCode();
        result = 31 * result + getPhone().hashCode();
        result = 31 * result + getCuisine().hashCode();
        result = 31 * result + getPriceRange().hashCode();
        temp = Double.doubleToLongBits(rating);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + numberOfRatings;
        result = 31 * result + getAddress().hashCode();
        result = 31 * result + getImageURL().hashCode();
        result = 31 * result + (isOpen ? 1 : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "Restaurant{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", cuisine='" + getCuisine() + '\'' +
                ", priceRange='" + getPriceRange() + '\'' +
                ", rating=" + rating +
                ", numberOfRatings=" + numberOfRatings +
                ", address='" + getAddress() + '\'' +
                ", imageURL='" + getImageURL() + '\'' +
                ", isOpen=" + isOpen +
                '}';
    }
} 