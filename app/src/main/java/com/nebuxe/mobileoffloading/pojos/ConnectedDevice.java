package com.nebuxe.mobileoffloading.pojos;

import java.io.Serializable;

public class ConnectedDevice implements Serializable {

    private String endpointId;
    private String endpointName;
    private DeviceStats deviceStats;
    private String requestStatus;


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

    public DeviceStats getDeviceStats() {
        return deviceStats;
    }

    public void setDeviceStats(DeviceStats deviceStats) {
        this.deviceStats = deviceStats;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

}
