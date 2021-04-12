package com.nebuxe.mobileoffloading.callbacks;

import com.google.android.gms.nearby.connection.Payload;

public interface PayloadListener {
    void onPayloadReceived(String endpointId, Payload payload);
}
