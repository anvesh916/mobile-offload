package com.nebuxe.mobileoffloading;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.nebuxe.mobileoffloading.adapters.WorkersAdapter;
import com.nebuxe.mobileoffloading.callbacks.ClientConnectionListener;
import com.nebuxe.mobileoffloading.callbacks.FusedLocationListener;
import com.nebuxe.mobileoffloading.callbacks.PayloadListener;
import com.nebuxe.mobileoffloading.callbacks.WorkerStatusListener;
import com.nebuxe.mobileoffloading.pojos.ConnectedDevice;
import com.nebuxe.mobileoffloading.pojos.DeviceStats;
import com.nebuxe.mobileoffloading.pojos.TPayload;
import com.nebuxe.mobileoffloading.pojos.WorkStatus;
import com.nebuxe.mobileoffloading.pojos.Worker;
import com.nebuxe.mobileoffloading.services.LocationMonitor;
import com.nebuxe.mobileoffloading.services.LocationService;
import com.nebuxe.mobileoffloading.services.NearbyConnectionsManager;
import com.nebuxe.mobileoffloading.services.WorkDistributor;
import com.nebuxe.mobileoffloading.services.WorkerStatusSubscriber;
import com.nebuxe.mobileoffloading.utilities.Constants;
import com.nebuxe.mobileoffloading.utilities.Device;
import com.nebuxe.mobileoffloading.utilities.FileWriter;
import com.nebuxe.mobileoffloading.utilities.Matrix;
import com.nebuxe.mobileoffloading.utilities.PayloadDataTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MasterActivity extends AppCompatActivity {

    private RecyclerView rvWorkers;

    private TextView tvWorkFinished, tvWorkTotal;

    private HashMap<String, WorkerStatusSubscriber> workerStatusSubscriberMap = new HashMap<>();

    private ArrayList<Worker> workers = new ArrayList<>();
    private WorkersAdapter workersAdapter;

    /* [row1 x cols1] * [row2 * cols2] */
    private int rows1 = 150;
    private int cols1 = 150;
    private int rows2 = cols1;
    private int cols2 = 150;

    private int[][] matrix1;
    private int[][] matrix2;

    private WorkDistributor workDistributor;

    private LocationService locationService;
    private Location lastAvailableLocation;

    private PayloadListener payloadListener;
    private ClientConnectionListener clientConnectionListener;

    private int workAmount;

    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        locationService = new LocationService(getApplicationContext());

        testRunOnMaster();

        unpackBundle();
        bindViews();
        setAdapters();
        setEventListeners();

        init();
        setupDeviceBatteryStatsCollector();
    }

    private void testRunOnMaster() {
        matrix1 = Matrix.buildMatrix(rows1, cols1);
        matrix2 = Matrix.buildMatrix(rows2, cols2);

        long startTime = System.currentTimeMillis();

        int[][] mul = new int[rows1][cols2];
        for (int i = 0; i < rows1; i++) {
            for (int j = 0; j < cols2; j++) {
                mul[i][j] = 0;
                for (int k = 0; k < cols1; k++) {
                    mul[i][j] += matrix1[i][k] * matrix2[k][j];
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

//        Toast.makeText(this, "Execution on master alone: " + (totalTime), Toast.LENGTH_SHORT).show();
        FileWriter.writeText(getApplicationContext(), "exec_time_master_alone.txt", false, totalTime + "ms");
    }

    private void setupDeviceBatteryStatsCollector() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                DeviceStats deviceStats = Device.getStats(getApplicationContext());
                Location location = LocationMonitor.getInstance(getApplicationContext()).getLastAvailableLocation();

                if (location != null) {
                    deviceStats.setLatitude(location.getLatitude());
                    deviceStats.setLongitude(location.getLongitude());
                    deviceStats.setLocationValid(true);
                }

                String deviceStatsStr = deviceStats.getBatteryLevel() + "%"
                        + "\t" + (deviceStats.isCharging() ? "CHARGING" : "NOT CHARGING");
                FileWriter.writeText(getApplicationContext(), "master_battery.txt", true, deviceStatsStr);

                handler.postDelayed(runnable, 5 * 1000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        NearbyConnectionsManager.getInstance(getApplicationContext()).registerPayloadListener(payloadListener);
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerClientConnectionListener(clientConnectionListener);
        startWorkerStatusSubscribers();

        handler.postDelayed(runnable, 5 * 1000);
        LocationMonitor.getInstance(getApplicationContext()).start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterPayloadListener(payloadListener);
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
        stopWorkerStatusSubscribers();

        handler.removeCallbacks(runnable);
        LocationMonitor.getInstance(getApplicationContext()).stop();
    }

    private void unpackBundle() {
        try {
            Bundle bundle = getIntent().getExtras();

            ArrayList<ConnectedDevice> connectedDevices = (ArrayList<ConnectedDevice>) bundle.getSerializable(Constants.CONNECTED_DEVICES);
            addToWorkers(connectedDevices);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void bindViews() {
        rvWorkers = findViewById(R.id.rv_workers);

        tvWorkFinished = findViewById(R.id.tv_work_finished);
        tvWorkTotal = findViewById(R.id.tv_work_total);
    }

    private void setEventListeners() {
        locationService.requestLocationUpdates(new FusedLocationListener() {
            @Override
            public void onLocationAvailable(Location location) {
                lastAvailableLocation = location;
            }
        });

        clientConnectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {

            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {

            }

            @Override
            public void onDisconnected(String endpointId) {
                updateWorkerConnectionStatus(endpointId, Constants.WorkStatus.DISCONNECTED);
                workDistributor.removeWorker(endpointId);
                NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(endpointId);
            }
        };
//
        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                try {
                    TPayload tPayload = PayloadDataTransformer.fromPayload(payload);
                    if (tPayload.getTag().equals(Constants.PayloadTags.DISCONNECTED)) {
                        Log.d("OFLOD", "DISCONN");

                        updateWorkerConnectionStatus(endpointId, Constants.WorkStatus.DISCONNECTED);
                        workDistributor.removeWorker(endpointId);
                        NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(endpointId);
                    }

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };

    }

    private void setAdapters() {
        workersAdapter = new WorkersAdapter(this, workers);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvWorkers.setLayoutManager(linearLayoutManager);

        rvWorkers.setAdapter(workersAdapter);
        workersAdapter.notifyDataSetChanged();
    }

    private void init() {
        tvWorkTotal.setText("Total Amount of work = " + rows1 * cols2);

        matrix1 = Matrix.buildMatrix(rows1, cols1);
        matrix2 = Matrix.buildMatrix(rows2, cols2);

        workDistributor = new WorkDistributor(getApplicationContext(), workers, matrix1, matrix2);
        workDistributor.start();

    }

    private void addToWorkers(ArrayList<ConnectedDevice> connectedDevices) {
        for (ConnectedDevice connectedDevice : connectedDevices) {
            Worker worker = new Worker();
            worker.setEndpointId(connectedDevice.getEndpointId());
            worker.setEndpointName(connectedDevice.getEndpointName());

            WorkStatus workStatus = new WorkStatus();
            workStatus.setStatus(Constants.WorkStatus.WORKING);

            worker.setWorkStatus(workStatus);
            worker.setDeviceStats(new DeviceStats());

            workers.add(worker);
        }
    }

    private void updateWorkerConnectionStatus(String endpointId, String status) {
        Log.d("DISCONNECTED----", endpointId);
        for (int i = 0; i < workers.size(); i++) {

            Log.d("DISCONNECTED--", workers.get(i).getEndpointId());
            if (workers.get(i).getEndpointId().equals(endpointId)) {
                workers.get(i).getWorkStatus().setStatus(status);
                workersAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    private void startWorkerStatusSubscribers() {
        for (Worker worker : workers) {
            if (workerStatusSubscriberMap.containsKey(worker.getEndpointId())) {
                continue;
            }

            WorkerStatusSubscriber workerStatusSubscriber = new WorkerStatusSubscriber(getApplicationContext(), worker.getEndpointId(), new WorkerStatusListener() {
                @Override
                public void onWorkStatusReceived(String endpointId, WorkStatus workStatus) {

                    if (workStatus.getStatus().equals(Constants.WorkStatus.DISCONNECTED)) {

                        Log.d("OFLOD", "DISCONN");

                        updateWorkerConnectionStatus(endpointId, Constants.WorkStatus.DISCONNECTED);
                        workDistributor.removeWorker(endpointId);
                        NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(endpointId);
                    } else {
                        updateWorkerStatus(endpointId, workStatus);
                    }

                    workDistributor.checkWorkCompletion(getWorkAmount());
                }

                @Override
                public void onDeviceStatsReceived(String endpointId, DeviceStats deviceStats) {
                    updateWorkerStatus(endpointId, deviceStats);

                    String deviceStatsStr = deviceStats.getBatteryLevel() + "%"
                            + "\t" + (deviceStats.isCharging() ? "CHARGING" : "NOT CHARGING")
                            + "\t\t" + deviceStats.getLatitude()
                            + "\t" + deviceStats.getLongitude();
                    FileWriter.writeText(getApplicationContext(), endpointId + ".txt", true, deviceStatsStr);

                    Log.d("OFLOD", "WORK AMOUNT: " + getWorkAmount());
                    workDistributor.checkWorkCompletion(getWorkAmount());
                }
            });

            workerStatusSubscriber.start();
            workerStatusSubscriberMap.put(worker.getEndpointId(), workerStatusSubscriber);
        }
    }

    private void stopWorkerStatusSubscribers() {
        for (Worker worker : workers) {
            WorkerStatusSubscriber workerStatusSubscriber = workerStatusSubscriberMap.get(worker.getEndpointId());
            if (workerStatusSubscriber != null) {
                workerStatusSubscriber.stop();
                workerStatusSubscriberMap.remove(worker.getEndpointId());
            }
        }
    }

    private void updateWorkerStatus(String endpointId, WorkStatus workStatus) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(endpointId)) {
                worker.setWorkStatus(workStatus);

                if (workStatus.getStatus().equals(Constants.WorkStatus.WORKING) && workDistributor.isNewWork(workStatus.getPartitionIndex())) {
                    workers.get(i).setWorkAmount(workers.get(i).getWorkAmount() + 1);
                    workAmount += 1;
                }

                workDistributor.updateWorkStatus(worker, workStatus);

                workersAdapter.notifyItemChanged(i);
                break;
            }
        }

        tvWorkFinished.setText("Amount of work finished = " + workAmount);
    }

    private void updateWorkerStatus(String endpointId, DeviceStats deviceStats) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(endpointId)) {
                worker.setDeviceStats(deviceStats);

                if (deviceStats.isLocationValid() && lastAvailableLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(lastAvailableLocation.getLatitude(), lastAvailableLocation.getLongitude(), deviceStats.getLatitude(), deviceStats.getLongitude(), results);

                    worker.setDistanceFromMaster(results[0]);
                }

                workersAdapter.notifyItemChanged(i);
            }
        }
    }

    private int getWorkAmount() {
        int sum = 0;
        for (Worker worker : workers) {
            sum += worker.getWorkAmount();

        }
        return sum;
    }

    private Worker getWorkerFromEndpointId(String endpointId) {
        for (Worker worker : workers) {
            if (worker.getEndpointId().equals(endpointId)) {
                return worker;
            }
        }

        return null;
    }
}