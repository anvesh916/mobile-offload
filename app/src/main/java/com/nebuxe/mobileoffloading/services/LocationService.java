package com.nebuxe.mobileoffloading.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.nebuxe.mobileoffloading.callbacks.FusedLocationListener;

public class LocationService {

    private Context context;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    public LocationService(Context context) {
        this.context = context;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void getLocation(FusedLocationListener fusedLocationListener) {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                fusedLocationListener.onLocationAvailable(location);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    @SuppressLint("MissingPermission")
    public void requestLocationUpdates(FusedLocationListener fusedLocationListener) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1 * 4000);

        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
//                for (Location location : locationResult.getLocations()) {
//                    fusedLocationListener.onLocationAvailable(location);
//                }
                fusedLocationListener.onLocationAvailable(locationResult.getLastLocation());
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, context.getMainLooper());
    }

    public void removeLocationUpdates() {
        if (this.locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(this.locationCallback);
        }
    }
}
