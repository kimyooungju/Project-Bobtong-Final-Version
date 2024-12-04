package com.example.project_bobtong;

public class Restaurant {
    private String id;
    private String title;
    private String address;
    private double mapx;
    private double mapy;
    private double latitude;
    private double longitude;
    private String category;

    public Restaurant() {
        // Default constructor required for calls to DataSnapshot.getValue(Restaurant.class)
    }

    public Restaurant(String id, String title, String address, double mapx, double mapy, double latitude, double longitude, String category) {
        this.id = id;
        this.title = title;
        this.address = address;
        this.mapx = mapx;
        this.mapy = mapy;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getMapx() {
        return mapx;
    }

    public void setMapx(double mapx) {
        this.mapx = mapx;
        this.longitude = mapx / 1000000.0;
    }

    public double getMapy() {
        return mapy;
    }

    public void setMapy(double mapy) {
        this.mapy = mapy;
        this.latitude = mapy / 1000000.0;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
