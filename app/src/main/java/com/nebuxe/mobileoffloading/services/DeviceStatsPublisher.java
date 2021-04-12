package com.nebuxe.mobileoffloading.services;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import com.nebuxe.mobileoffloading.pojos.DeviceStats;
import com.nebuxe.mobileoffloading.pojos.TPayload;
import com.nebuxe.mobileoffloading.utilities.Constants;
import com.nebuxe.mobileoffloading.utilities.Device;

public class DeviceStatsPublisher {

    private Context context;
    private String endpointId;
    private int intervalInMillis;

    private Handler handler;
    private Runnable runnable;

    public DeviceStatsPublisher(Context context, String endpointId, int intervalInMillis) {
        this.context = context;
        this.endpointId = endpointId;
        this.intervalInMillis = intervalInMillis;

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                publish();
                handler.postDelayed(runnable, intervalInMillis);
            }
        };
    }

    private void publish() {
        DeviceStatsPublisher.publish(this.context, this.endpointId);
    }

    public void start() {
        handler.postDelayed(runnable, intervalInMillis);
        LocationMonitor.getInstance(context).start();
    }

    public void stop() {
        handler.removeCallbacks(runnable);
        LocationMonitor.getInstance(context).stop();
    }

    public static void publish(Context context, String endpointId) {
        Location location = LocationMonitor.getInstance(context).getLastAvailableLocation();

        DeviceStats deviceStats = Device.getStats(context);
        if (location != null) {
            deviceStats.setLatitude(location.getLatitude());
            deviceStats.setLongitude(location.getLongitude());
            deviceStats.setLocationValid(true);
        }

        TPayload tPayload = new TPayload();
        tPayload.setTag(Constants.PayloadTags.DEVICE_STATS);
        tPayload.setData(deviceStats);

        Communicator.sendToDevice(context, endpointId, tPayload);

        Log.d("OFLOD", "DEVICE STATUS");
        Log.d("OFLOD", deviceStats.getBatteryLevel() + "");
        Log.d("OFLOD", deviceStats.isCharging() + "");
    }

}
