package com.nebuxe.mobileoffloading.callbacks;

import com.nebuxe.mobileoffloading.pojos.DeviceStats;
import com.nebuxe.mobileoffloading.pojos.WorkStatus;

public interface WorkerStatusListener {
    void onWorkStatusReceived(String endpointId, WorkStatus workStatus);

    void onDeviceStatsReceived(String endpointId, DeviceStats deviceStats);
}
