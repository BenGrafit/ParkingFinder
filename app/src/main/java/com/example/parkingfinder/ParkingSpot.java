package com.example.parkingfinder;

public class ParkingSpot {
    private boolean IsEmpty;
    private double x;
    private double y;
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // 🔹 No-argument constructor required by Firestore
    public ParkingSpot() {
        this.IsEmpty = true; // optional default
        this.x = 0.0;
        this.y = 0.0;
        this.id = null;
    }

    // 🔹 Constructor with coordinates
    public ParkingSpot(double x, double y) {
        this.IsEmpty = true;
        this.x = x;
        this.y = y;
        this.id = null;

    }

    // Getters and setters


    public boolean isEmpty() {
        return IsEmpty;
    }

    public void setEmpty(boolean IsEmpty) {
        this.IsEmpty = IsEmpty;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
