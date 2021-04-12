package com.nebuxe.mobileoffloading;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.nebuxe.mobileoffloading.callbacks.ClientConnectionListener;
import com.nebuxe.mobileoffloading.services.Advertiser;
import com.nebuxe.mobileoffloading.services.DeviceStatsPublisher;
import com.nebuxe.mobileoffloading.services.NearbyConnectionsManager;
import com.nebuxe.mobileoffloading.utilities.Constants;
import com.nebuxe.mobileoffloading.views.DialogView;

public class WorkAdvertisementActivity extends AppCompatActivity {

    private TextView tvMessage;
    private Button bAccept, bReject;

    private Advertiser advertiser;
    private String clientId;

    private Dialog connectionRequestDialog;

    private ClientConnectionListener clientConnectionListener;
    private String masterEndpointId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_advertisement);

        advertiser = new Advertiser(this);
        clientId = (Build.MANUFACTURER + " " + Build.MODEL);

        setupDialogs();
        bindViews();
        setEventListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        NearbyConnectionsManager.getInstance(getApplicationContext()).registerClientConnectionListener(clientConnectionListener);
        advertiser.start(clientId);
    }

    @Override
    protected void onPause() {
        super.onPause();

        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
        advertiser.stop();
    }

    private void setupDialogs() {
        connectionRequestDialog = new DialogView(this).getDialog(R.layout.layout_dialog_client_identity, false);
    }

    private void bindViews() {
        tvMessage = connectionRequestDialog.findViewById(R.id.tv_message);
        bAccept = connectionRequestDialog.findViewById(R.id.b_accept);
        bReject = connectionRequestDialog.findViewById(R.id.b_reject);
    }

    private void setEventListeners() {

        bAccept.setOnClickListener(view -> {
            NearbyConnectionsManager.getInstance(getApplicationContext()).acceptConnection(masterEndpointId);

            DeviceStatsPublisher.publish(getApplicationContext(), masterEndpointId);
            advertiser.stop();
            startWorkerActivity();
            finish();
        });

        bReject.setOnClickListener(view -> {
            NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(masterEndpointId);
            connectionRequestDialog.dismiss();
        });

        clientConnectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                Log.d("OFLOD", "CONNECTION INITIATED");
                Log.d("OFLOD", endpointId);
                Log.d("OFLOD", connectionInfo.getEndpointName() + " " + connectionInfo.getAuthenticationToken());
                masterEndpointId = endpointId;

                connectionRequestDialog.show();
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {

                Log.d("OFLOD", "CONNECTION RESULT");
                Log.d("OFLOD", endpointId);
                Log.d("OFLOD", connectionResolution.getStatus().toString() + "");

                Toast.makeText(WorkAdvertisementActivity.this, "CONNECTED", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnected(String endpointId) {
                Log.d("OFLOD", "DISCONNECTED");
                Log.d("OFLOD", endpointId);

                Toast.makeText(WorkAdvertisementActivity.this, "DISCONNECTED", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void startWorkerActivity() {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.MASTER_ENDPOINT_ID, masterEndpointId);

        Intent intent = new Intent(getApplicationContext(), WorkerActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);
        finish();
    }

}