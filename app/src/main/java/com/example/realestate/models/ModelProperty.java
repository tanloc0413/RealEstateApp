package com.example.realestate.models;

public class ModelProperty {
    String id, uid, purpose, category, subcategory, areaSizeUnit;
    String title, description, email, phoneCode, phoneNumber;
    String country, city, state, address, status;
    long floors, bedRooms, bathRooms, timestamp;
    Double latitude, longitude, areaSize, price;

    public ModelProperty() {
    }

    public ModelProperty(String id, String uid, String purpose, String category,
                         String subcategory, String areaSizeUnit, String title, String description,
                         String email, String phoneCode, String phoneNumber, String country,
                         String city, String state, String address, String status,
                         long floors, long bedRooms, long bathRooms, long timestamp,
                         Double latitude, Double longitude, Double areaSize, Double price) {
        this.id = id;
        this.uid = uid;
        this.purpose = purpose;
        this.category = category;
        this.subcategory = subcategory;
        this.areaSizeUnit = areaSizeUnit;
        this.title = title;
        this.description = description;
        this.email = email;
        this.phoneCode = phoneCode;
        this.phoneNumber = phoneNumber;
        this.country = country;
        this.city = city;
        this.state = state;
        this.address = address;
        this.status = status;
        this.floors = floors;
        this.bedRooms = bedRooms;
        this.bathRooms = bathRooms;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaSize = areaSize;
        this.price = price;
    }
}
