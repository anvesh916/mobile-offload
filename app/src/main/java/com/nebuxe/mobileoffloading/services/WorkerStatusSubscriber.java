package com.nebuxe.mobileoffloading.services;

import android.content.Context;

import com.google.android.gms.nearby.connection.Payload;
import com.nebuxe.mobileoffloading.callbacks.PayloadListener;
import com.nebuxe.mobileoffloading.callbacks.WorkerStatusListener;
import com.nebuxe.mobileoffloading.pojos.DeviceStats;
import com.nebuxe.mobileoffloading.pojos.TPayload;
import com.nebuxe.mobileoffloading.pojos.WorkStatus;
import com.nebuxe.mobileoffloading.utilities.Constants;
import com.nebuxe.mobileoffloading.utilities.PayloadDataTransformer;

import java.io.IOException;

public class WorkerStatusSubscriber {

    private Context context;
    private String endpointId;
    private PayloadListener payloadListener;
    private WorkerStatusListener workerStatusListener;

    public WorkerStatusSubscriber(Context context, String endpointId, WorkerStatusListener workerStatusListener) {
        this.context = context;
        this.endpointId = endpointId;
        this.workerStatusListener = workerStatusListener;
    }

    public void start() {
        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                try {
                    TPayload tPayload = (TPayload) PayloadDataTransformer.fromPayload(payload);
                    String payloadTag = tPayload.getTag();

                    if (payloadTag.equals(Constants.PayloadTags.WORK_STATUS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onWorkStatusReceived(endpointId, (WorkStatus) tPayload.getData());
                        }
                    } else if (payloadTag.equals(Constants.PayloadTags.DEVICE_STATS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onDeviceStatsReceived(endpointId, (DeviceStats) tPayload.getData());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };

        NearbyConnectionsManager.getInstance(context).registerPayloadListener(payloadListener);
        NearbyConnectionsManager.getInstance(context).acceptConnection(endpointId);
    }

    public void stop() {
        NearbyConnectionsManager.getInstance(context).unregisterPayloadListener(payloadListener);
    }
}
