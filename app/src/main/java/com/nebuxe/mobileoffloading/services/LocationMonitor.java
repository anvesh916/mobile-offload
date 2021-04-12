package com.nebuxe.mobileoffloading.services;

import android.content.Context;
import android.location.Location;

import com.nebuxe.mobileoffloading.callbacks.FusedLocationListener;

public class LocationMonitor {

    private Context context;
    private int intervalInMillis;

    private LocationService locationService;

    private Location lastAvailableLocation;

    private static LocationMonitor locationMonitor;

    public LocationMonitor(Context context) {
        this.context = context;
        this.intervalInMillis = 4 * 1000;

        this.locationService = new LocationService(this.context);
    }

    public static LocationMonitor getInstance(Context context) {
        if (locationMonitor == null) {
            locationMonitor = new LocationMonitor(context);
        }

        return locationMonitor;
    }

    public void start() {
        locationService.requestLocationUpdates(new FusedLocationListener() {
            @Override
            public void onLocationAvailable(Location location) {
                lastAvailableLocation = location;
            }
        });
    }

    public void stop() {
        locationService.removeLocationUpdates();
    }

    public Location getLastAvailableLocation() {
        return this.lastAvailableLocation;
    }
}
