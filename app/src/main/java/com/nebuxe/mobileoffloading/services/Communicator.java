package com.nebuxe.mobileoffloading.services;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;
import com.nebuxe.mobileoffloading.pojos.TPayload;
import com.nebuxe.mobileoffloading.utilities.PayloadDataTransformer;

import java.io.IOException;

public class Communicator {
    public static void sendToDevice(Context context, String endpointId, TPayload tPayload) {
        try {
            Payload payload = PayloadDataTransformer.toPayload(tPayload);
            Communicator.sendToDevice(context, endpointId, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendToDevice(Context context, String endpointId, byte[] data) {
        Payload payload = Payload.fromBytes(data);
        Communicator.sendToDevice(context, endpointId, payload);
    }

    public static void sendToDevice(Context context, String endpointId, Payload payload) {
        Nearby.getConnectionsClient(context).sendPayload(endpointId, payload);
    }
}
