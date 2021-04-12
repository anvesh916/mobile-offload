package com.nebuxe.mobileoffloading.services;

import android.content.Context;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.Strategy;

public class Advertiser {

    private Context context;
    private AdvertisingOptions advertisingOptions;

    public Advertiser(Context context) {
        this.context = context;
        this.advertisingOptions =
                new AdvertisingOptions.Builder()
                        .setStrategy(Strategy.P2P_CLUSTER)
                        .build();
    }

    public void start(String clientId) {
        NearbyConnectionsManager.getInstance(context).advertise(clientId, advertisingOptions);
    }

    public void stop() {
        Nearby.getConnectionsClient(context).stopAdvertising();
    }
}
