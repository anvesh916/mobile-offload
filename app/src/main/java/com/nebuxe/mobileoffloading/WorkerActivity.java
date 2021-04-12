package com.nebuxe.mobileoffloading;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.nebuxe.mobileoffloading.callbacks.ClientConnectionListener;
import com.nebuxe.mobileoffloading.callbacks.PayloadListener;
import com.nebuxe.mobileoffloading.pojos.TPayload;
import com.nebuxe.mobileoffloading.pojos.WorkData;
import com.nebuxe.mobileoffloading.pojos.WorkStatus;
import com.nebuxe.mobileoffloading.services.Communicator;
import com.nebuxe.mobileoffloading.services.DeviceStatsPublisher;
import com.nebuxe.mobileoffloading.services.NearbyConnectionsManager;
import com.nebuxe.mobileoffloading.utilities.Constants;
import com.nebuxe.mobileoffloading.utilities.Matrix;
import com.nebuxe.mobileoffloading.utilities.PayloadDataTransformer;

import java.io.IOException;
import java.util.HashSet;

public class WorkerActivity extends AppCompatActivity {

    private TextView tvWorkStatus, tvWorkFinished;
    private Button bDisconnect, bClose;
    private LottieAnimationView processingAnimation;

    private String masterEndpointId;
    private DeviceStatsPublisher deviceStatsPublisher;
    private PayloadListener payloadListener;
    private ClientConnectionListener clientConnectionListener;

    private int workAmount;

    private int currentPartitionIndex;
    private HashSet<Integer> finishedWork = new HashSet<>();

    private boolean disconnected = false;
    private boolean allWorkDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);

        unpackBundle();
        bindViews();
        setEventListeners();

        startDeviceStatsPublisher();
        connectToMaster();
    }

    @Override
    protected void onResume() {
        super.onResume();

        NearbyConnectionsManager.getInstance(getApplicationContext()).registerPayloadListener(payloadListener);
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerClientConnectionListener(clientConnectionListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterPayloadListener(payloadListener);
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
    }

    private void unpackBundle() {
        try {
            Bundle bundle = getIntent().getExtras();

            this.masterEndpointId = bundle.getString(Constants.MASTER_ENDPOINT_ID);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void bindViews() {
        tvWorkStatus = findViewById(R.id.tv_work_status);
        tvWorkFinished = findViewById(R.id.tv_work_finished);

        bDisconnect = findViewById(R.id.b_disconnect);
        bClose = findViewById(R.id.b_close);

        processingAnimation = findViewById(R.id.processing_animation);
    }

    private void setEventListeners() {

        bDisconnect.setOnClickListener(view -> {

//            TPayload tPayload = new TPayload();
//            tPayload.setTag(Constants.PayloadTags.DISCONNECTED);
//
//            Communicator.sendToDevice(getApplicationContext(), masterEndpointId, tPayload);
//
//            deviceStatsPublisher.stop();


            WorkStatus workStatus = new WorkStatus();
            workStatus.setPartitionIndex(currentPartitionIndex);
            workStatus.setStatus(Constants.WorkStatus.DISCONNECTED);

            tvWorkStatus.setText("DISCONNECTED...");

            TPayload tPayload1 = new TPayload();
            tPayload1.setTag(Constants.PayloadTags.WORK_STATUS);
            tPayload1.setData(workStatus);

            Communicator.sendToDevice(getApplicationContext(), masterEndpointId, tPayload1);
            deviceStatsPublisher.stop();

            bClose.setVisibility(View.VISIBLE);
            bDisconnect.setVisibility(View.GONE);

            disconnected = true;

            NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterPayloadListener(payloadListener);
            NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
        });

        bClose.setOnClickListener(view -> {
            NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(masterEndpointId);
            finish();
        });

        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {

                if (allWorkDone) {
                    return;
                }

                try {
                    TPayload tPayload = PayloadDataTransformer.fromPayload(payload);
                    if (tPayload.getTag().equals(Constants.PayloadTags.WORK_DATA)) {

                        if (disconnected) {
                            WorkData workData = (WorkData) tPayload.getData();

                            WorkStatus workStatus = new WorkStatus();
                            workStatus.setPartitionIndex(workData.getPartitionIndex());
                            workStatus.setStatus(Constants.WorkStatus.DISCONNECTED);

                            tvWorkStatus.setText("DISCONNECTED...");

                            TPayload tPayload1 = new TPayload();
                            tPayload1.setTag(Constants.PayloadTags.WORK_STATUS);
                            tPayload1.setData(workStatus);

                            Communicator.sendToDevice(getApplicationContext(), masterEndpointId, tPayload1);
                            return;
                        }

                        tvWorkStatus.setText("RECEIVED WORK...");

                        WorkData workData = (WorkData) tPayload.getData();
                        int dotProduct = Matrix.dotProduct(workData.getRows(), workData.getCols());

                        if (!finishedWork.contains(workData.getPartitionIndex())) {
                            workAmount += 1;
                        }

                        finishedWork.add(workData.getPartitionIndex());

                        currentPartitionIndex = workData.getPartitionIndex();

                        WorkStatus workStatus = new WorkStatus();
                        workStatus.setPartitionIndex(workData.getPartitionIndex());
                        workStatus.setStatus(Constants.WorkStatus.WORKING);
                        workStatus.setResult(dotProduct);

//                        tvWorkStatus.setText("FINISHED AND WAITING FOR WORK...");
                        tvWorkStatus.setText("WORKING...");
                        tvWorkFinished.setText("Amount of work finished = " + workAmount);

                        TPayload tPayload1 = new TPayload();
                        tPayload1.setTag(Constants.PayloadTags.WORK_STATUS);
                        tPayload1.setData(workStatus);

                        Communicator.sendToDevice(getApplicationContext(), masterEndpointId, tPayload1);
                    } else if (tPayload.getTag().equals(Constants.PayloadTags.FAREWELL)) {
                        allWorkDone = true;

                        tvWorkStatus.setText("ALL WORK IS DONE");
                        processingAnimation.pauseAnimation();

                        WorkStatus workStatus = new WorkStatus();
                        workStatus.setStatus(Constants.WorkStatus.FINISHED);

                        TPayload tPayload1 = new TPayload();
                        tPayload1.setTag(Constants.PayloadTags.WORK_STATUS);
                        tPayload1.setData(workStatus);

                        Communicator.sendToDevice(getApplicationContext(), masterEndpointId, tPayload1);

                        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
                        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterPayloadListener(payloadListener);

                    } else if (tPayload.getTag().equals(Constants.PayloadTags.DISCONNECTED)) {

                        tvWorkStatus.setText("NO MORE WORK");
                        processingAnimation.pauseAnimation();
                        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
                        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterPayloadListener(payloadListener);
                        NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(endpointId);

                    }

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };

        clientConnectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {

            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {

            }

            @Override
            public void onDisconnected(String endpointId) {

                tvWorkStatus.setText("NO MORE WORK");
                processingAnimation.pauseAnimation();
                NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
            }
        };
    }

    private void startDeviceStatsPublisher() {
        deviceStatsPublisher = new DeviceStatsPublisher(getApplicationContext(), masterEndpointId, 5 * 1000);
        deviceStatsPublisher.start();
    }

    private void connectToMaster() {
        NearbyConnectionsManager.getInstance(getApplicationContext()).acceptConnection(masterEndpointId);
    }
}