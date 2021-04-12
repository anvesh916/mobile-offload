package com.nebuxe.mobileoffloading.pojos;

import java.io.Serializable;

public class Worker implements Serializable {

    private String endpointId;
    private String endpointName;

    private DeviceStats deviceStats;
    private WorkStatus workStatus;

    private int workAmount;
    private float distanceFromMaster;

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public WorkStatus getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(WorkStatus workStatus) {
        this.workStatus = workStatus;
    }

    public DeviceStats getDeviceStats() {
        return deviceStats;
    }

    public void setDeviceStats(DeviceStats deviceStats) {
        this.deviceStats = deviceStats;
    }

    public int getWorkAmount() {
        return workAmount;
    }

    public void setWorkAmount(int workAmount) {
        this.workAmount = workAmount;
    }

    public float getDistanceFromMaster() {
        return distanceFromMaster;
    }

    public void setDistanceFromMaster(float distanceFromMaster) {
        this.distanceFromMaster = distanceFromMaster;
    }
}
