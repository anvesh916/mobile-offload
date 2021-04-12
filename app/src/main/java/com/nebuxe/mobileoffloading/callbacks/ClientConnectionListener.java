package com.nebuxe.mobileoffloading.callbacks;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;

public interface ClientConnectionListener {
    void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo);

    void onConnectionResult(String endpointId, ConnectionResolution connectionResolution);

    void onDisconnected(String endpointId);
}
