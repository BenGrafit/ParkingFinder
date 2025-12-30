package com.example.parkingfinder;


import java.util.List;

public interface ParkingSpotListener {


    void onUpdate(List<ParkingSpot> updatedList);


    void onError(Exception e);
}

