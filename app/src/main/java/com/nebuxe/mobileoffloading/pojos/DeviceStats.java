package com.nebuxe.mobileoffloading.pojos;

import java.io.Serializable;

public class DeviceStats implements Serializable {

    private int totalCapacity;
    private int batteryLevel;
    private boolean charging;

    private double latitude;
    private double longitude;

    private boolean locationValid;

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
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

    public boolean isLocationValid() {
        return locationValid;
    }

    public void setLocationValid(boolean locationValid) {
        this.locationValid = locationValid;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
}
