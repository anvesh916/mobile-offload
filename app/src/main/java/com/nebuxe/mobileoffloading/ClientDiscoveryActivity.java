package com.nebuxe.mobileoffloading;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.nebuxe.mobileoffloading.adapters.ConnectedDevicesAdapter;
import com.nebuxe.mobileoffloading.callbacks.ClientConnectionListener;
import com.nebuxe.mobileoffloading.callbacks.PayloadListener;
import com.nebuxe.mobileoffloading.pojos.ConnectedDevice;
import com.nebuxe.mobileoffloading.pojos.DeviceStats;
import com.nebuxe.mobileoffloading.pojos.TPayload;
import com.nebuxe.mobileoffloading.services.ClientDiscovery;
import com.nebuxe.mobileoffloading.services.Communicator;
import com.nebuxe.mobileoffloading.services.NearbyConnectionsManager;
import com.nebuxe.mobileoffloading.services.WorkDistributor;
import com.nebuxe.mobileoffloading.utilities.Constants;
import com.nebuxe.mobileoffloading.utilities.PayloadDataTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientDiscoveryActivity extends AppCompatActivity {

    private Button bDoneDiscovering;
    private RecyclerView rvConnectedDevices;

    private ClientDiscovery clientDiscovery;

    private List<ConnectedDevice> connectedDevices = new ArrayList<>();
    private ConnectedDevicesAdapter connectedDevicesAdapter;

    private ClientConnectionListener clientConnectionListener;
    private PayloadListener payloadListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_discovery);

        bindViews();
        setAdapters();
        setEventListeners();
        startClientDiscovery();
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

    private void bindViews() {
        bDoneDiscovering = findViewById(R.id.b_done_discovering);
        rvConnectedDevices = findViewById(R.id.rv_connected_devices);
    }

    private void setEventListeners() {
        bDoneDiscovering.setOnClickListener(view -> {
            ArrayList<ConnectedDevice> readyDevices = getDevicesInReadyState();
            if (readyDevices.size() == 0) {
                Toast.makeText(this, "No workers found", Toast.LENGTH_SHORT).show();
                onBackPressed();
            } else {
                clientDiscovery.stop();
                startMasterActivity(readyDevices);
                finish();
            }
        });

        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {

                try {
                    TPayload tPayload = PayloadDataTransformer.fromPayload(payload);
                    if (tPayload.getTag().equals(Constants.PayloadTags.DEVICE_STATS)) {
                        updateDeviceStats(endpointId, (DeviceStats) tPayload.getData());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };

        clientConnectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                NearbyConnectionsManager.getInstance(getApplicationContext()).acceptConnection(endpointId);
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
                int statusCode = connectionResolution.getStatus().getStatusCode();

                if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    updateConnectedDeviceRequestStatus(endpointId, Constants.RequestStatus.ACCEPTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
                    updateConnectedDeviceRequestStatus(endpointId, Constants.RequestStatus.REJECTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_ERROR) {
                    removeConnectedDevice(endpointId);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                removeConnectedDevice(endpointId);
            }
        };
    }

    private void setAdapters() {
        connectedDevicesAdapter = new ConnectedDevicesAdapter(this, connectedDevices);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvConnectedDevices.setLayoutManager(linearLayoutManager);

        rvConnectedDevices.setAdapter(connectedDevicesAdapter);
        connectedDevicesAdapter.notifyDataSetChanged();
    }


    private void startClientDiscovery() {
        EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.d("OFLOD", "ENDPOINT FOUND");
                Log.d("OFLOD", endpointId);
                Log.d("OFLOD", discoveredEndpointInfo.getServiceId() + " " + discoveredEndpointInfo.getEndpointName());

                ConnectedDevice connectedDevice = new ConnectedDevice();
                connectedDevice.setEndpointId(endpointId);
                connectedDevice.setEndpointName(discoveredEndpointInfo.getEndpointName());
                connectedDevice.setRequestStatus(Constants.RequestStatus.PENDING);
                connectedDevice.setDeviceStats(new DeviceStats());

                connectedDevices.add(connectedDevice);
                connectedDevicesAdapter.notifyItemChanged(connectedDevices.size() - 1);

                NearbyConnectionsManager.getInstance(getApplicationContext()).requestConnection(endpointId, "MASTER");
            }

            @Override
            public void onEndpointLost(@NonNull String endpointId) {
                Log.d("OFLOD", "ENDPOINT LOST");
                Log.d("OFLOD", endpointId);

                removeConnectedDevice(endpointId);
            }
        };

        clientDiscovery = new ClientDiscovery(this);
        clientDiscovery.start(endpointDiscoveryCallback);
    }

    private ArrayList<ConnectedDevice> getDevicesInReadyState() {
        ArrayList<ConnectedDevice> res = new ArrayList<>();
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getRequestStatus().equals(Constants.RequestStatus.ACCEPTED)) {
                if (connectedDevices.get(i).getDeviceStats().getBatteryLevel() > WorkDistributor.Thresholds.MINIMUM_BATTERY_LEVEL) {
                    res.add(connectedDevices.get(i));
                } else {
//                    NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(connectedDevices.get(i).getEndpointId());

                    TPayload tPayload = new TPayload();
                    tPayload.setTag(Constants.PayloadTags.DISCONNECTED);

                    Communicator.sendToDevice(getApplicationContext(), connectedDevices.get(i).getEndpointId(), tPayload);
                }
            } else {
//                NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(connectedDevices.get(i).getEndpointId());

                Log.d("OFLOD", "LOOPING");
                TPayload tPayload = new TPayload();
                tPayload.setTag(Constants.PayloadTags.DISCONNECTED);

                Communicator.sendToDevice(getApplicationContext(), connectedDevices.get(i).getEndpointId(), tPayload);
            }

        }
        return res;
    }

    private void removeConnectedDevice(String endpointId) {
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getEndpointId().equals(endpointId)) {
                connectedDevices.remove(i);
                connectedDevicesAdapter.notifyItemChanged(i);
                i--;
            }
        }
    }

    private void updateConnectedDeviceRequestStatus(String endpointId, String status) {
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getEndpointId().equals(endpointId)) {
                connectedDevices.get(i).setRequestStatus(status);
                connectedDevicesAdapter.notifyItemChanged(i);
            }
        }
    }

    private void updateDeviceStats(String endpointId, DeviceStats deviceStats) {
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getEndpointId().equals(endpointId)) {
                connectedDevices.get(i).setDeviceStats(deviceStats);
                connectedDevices.get(i).setRequestStatus(Constants.RequestStatus.ACCEPTED);
                connectedDevicesAdapter.notifyItemChanged(i);
            }
        }
    }

    private void startMasterActivity(ArrayList<ConnectedDevice> connectedDevices) {
        Intent intent = new Intent(getApplicationContext(), MasterActivity.class);

        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.CONNECTED_DEVICES, connectedDevices);
        intent.putExtras(bundle);

        startActivity(intent);
    }
}