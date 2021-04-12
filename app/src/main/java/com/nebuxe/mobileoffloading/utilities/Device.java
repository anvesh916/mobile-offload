package com.nebuxe.mobileoffloading.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.nebuxe.mobileoffloading.pojos.DeviceStats;

public class Device {

    public static int getBatteryLevel(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public static boolean isPluggedIn(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugStatus = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugStatus == BatteryManager.BATTERY_STATUS_CHARGING
                || plugStatus == BatteryManager.BATTERY_PLUGGED_AC
                || plugStatus == BatteryManager.BATTERY_PLUGGED_USB
                || plugStatus == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    public static DeviceStats getStats(Context context) {
        int batteryLevel = Device.getBatteryLevel(context);
        boolean charging = Device.isPluggedIn(context);

        DeviceStats deviceStats = new DeviceStats();
        deviceStats.setBatteryLevel(batteryLevel);
        deviceStats.setCharging(charging);

        return deviceStats;
    }
}
