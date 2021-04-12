package com.nebuxe.mobileoffloading.services;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Payload;
import com.nebuxe.mobileoffloading.pojos.TPayload;
import com.nebuxe.mobileoffloading.pojos.WorkData;
import com.nebuxe.mobileoffloading.pojos.WorkStatus;
import com.nebuxe.mobileoffloading.pojos.Worker;
import com.nebuxe.mobileoffloading.utilities.Constants;
import com.nebuxe.mobileoffloading.utilities.FileWriter;
import com.nebuxe.mobileoffloading.utilities.Matrix;
import com.nebuxe.mobileoffloading.utilities.PayloadDataTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class WorkDistributor {

    private Context context;
    private ArrayList<Worker> workers = new ArrayList<>();

    private int[][] matrix1, matrix2, matrix2T;
    private int rows1, cols1, rows2, cols2;

    private int totalPartitions;

    private Hashtable<Integer, Integer> partitionResults = new Hashtable<>();

    private PriorityQueue<Worker> workerQueue;              /* Ready queue for the workers */
    private BlockingDeque<Integer> workQueue = new LinkedBlockingDeque<>();  /* Ready queue for the work */


    private boolean farewell = false;

    private Handler handler;
    private Runnable runnable;

    private long startTime;

    public WorkDistributor(Context context, ArrayList<Worker> workers, int[][] matrix1, int[][] matrix2) {
        this.context = context;
        this.workers.addAll(workers);

        this.matrix1 = matrix1;
        this.matrix2 = matrix2;
        this.matrix2T = Matrix.transpose(matrix2);

        this.rows1 = matrix1.length;
        this.cols1 = matrix1[0].length;
        this.rows2 = matrix2.length;
        this.cols2 = matrix2[0].length;

        this.workerQueue = new PriorityQueue<Worker>(this.workers.size(), new WorkerComparator());
        this.totalPartitions = this.rows1 * this.cols2;
    }

    public void start() {
        startTime = System.currentTimeMillis();

        addWorkersToQueue();
        addWorkToQueue();
        initiateWorkAssignment();

        startWorkScheduler();
    }

    private void addWorkersToQueue() {
        for (Worker worker : workers) {
            workerQueue.add(worker);
        }
    }

    private void addWorkersToQueue(Worker worker) {
        workerQueue.add(worker);
    }

    private void addWorkToQueue() {
        for (int i = 0; i < totalPartitions; i++) {
            if (!partitionResults.containsKey(i)) {
                workQueue.add(i);
            }
        }
    }

    public void addWorkToQueue(int partitionIndex) {
        if (!partitionResults.containsKey(partitionIndex)) {
            workQueue.add(partitionIndex);
        }
    }

    private void initiateWorkAssignment() {
        for (int i = 0; i < workerQueue.size(); i++) {
            assignWork();
        }
    }

    private void startWorkScheduler() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (partitionResults.size() == totalPartitions) {
                    sendFarewellToWorkers();
                } else {
                    addWorkToQueue();
                    assignWork();
                    handler.postDelayed(runnable, 1000);
                }
            }
        };

        handler.postDelayed(runnable, 1000);
    }

    public void assignWork() {

        if (workerQueue.size() > 0 && workQueue.size() > 0 && partitionResults.size() != totalPartitions) {
            Worker worker = workerQueue.poll();
            int partitionIndex = workQueue.poll();

            while (!workers.contains(worker)) {
                if (workerQueue.size() == 0) {
                    return;
                }
                worker = workerQueue.poll();
            }

            while (partitionResults.containsKey(partitionIndex)) {
                if (workQueue.size() == 0) {
                    return;
                }
                partitionIndex = workQueue.poll();
            }

            int row1 = partitionIndex / cols2;
            int col2 = partitionIndex % cols2;

            int[] rows = matrix1[row1];
            int[] cols = matrix2T[col2];

            WorkData workData = new WorkData();
            workData.setPartitionIndex(partitionIndex);
            workData.setRows(rows);
            workData.setCols(cols);

            TPayload tPayload = new TPayload();
            tPayload.setTag(Constants.PayloadTags.WORK_DATA);
            tPayload.setData(workData);

            try {
                Payload payload = PayloadDataTransformer.toPayload(tPayload);
                Communicator.sendToDevice(context, worker.getEndpointId(), payload);
            } catch (IOException e) {

                if (isWorkerActive(worker)) {
                    workerQueue.add(worker);
                }
                workQueue.add(partitionIndex);

                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();

                e.printStackTrace();
            }
        }
    }

    public void setWorkers(ArrayList<Worker> workers) {
        this.workers.clear();
        this.workers.addAll(workers);
    }


    public void removeWorker(String endpointId) {
        for (int i = 0; i < workers.size(); i++) {
            if (workers.get(i).getEndpointId().equals(endpointId)) {
                workers.remove(i);
                break;
            }
        }
    }

    private boolean isWorkerActive(Worker worker) {
        /* Check if the worker is in workers list */

        for (int i = 0; i < workers.size(); i++) {
            if (worker.getEndpointId().equals(workers.get(i).getEndpointId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isNewWork(int partitionIndex) {
        return !partitionResults.containsKey(partitionIndex);
    }

    public void updateWorkStatus(Worker worker, WorkStatus workStatus) {
        if (partitionResults.size() == totalPartitions) {
            return;
        }

        if (worker == null || workStatus == null) {
            return;
        }

        if (workStatus.getStatus().equals(Constants.WorkStatus.WORKING)) {
            partitionResults.put(workStatus.getPartitionIndex(), workStatus.getResult());
        }

        if (workStatus.getStatus().equals(Constants.WorkStatus.FAILED) || workStatus.getStatus().equals(Constants.WorkStatus.DISCONNECTED)) {
            addWorkToQueue(workStatus.getPartitionIndex());
//            addWorkToQueue();
        }

        if (!workStatus.getStatus().equals(Constants.WorkStatus.DISCONNECTED)) {
            addWorkersToQueue(worker);
        }

        if (partitionResults.size() != totalPartitions) {
            assignWork();
        } else {
            sendFarewellToWorkers();
        }
    }

    public void checkWorkCompletion(int workAmount) {
        if (workAmount == totalPartitions) {
            sendFarewellToWorkers();
        } else if (partitionResults.size() == totalPartitions) {
            sendFarewellToWorkers();
        }
    }

    private void sendFarewellToWorkers() {
        if (farewell) {
            return;
        }

        farewell = true;
        handler.removeCallbacks(runnable);

        for (Worker worker : workers) {
            if (!worker.getWorkStatus().getStatus().equals(Constants.WorkStatus.DISCONNECTED)) {

                TPayload tPayload = new TPayload();
                tPayload.setTag(Constants.PayloadTags.FAREWELL);

                Communicator.sendToDevice(context, worker.getEndpointId(), tPayload);
                NearbyConnectionsManager.getInstance(context).rejectConnection(worker.getEndpointId());
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

//        Toast.makeText(context, "Distributed approach: " + totalTime, Toast.LENGTH_SHORT).show();
        FileWriter.writeText(context, "exec_time_dist_approach.txt", false, totalTime + "ms");


        FileWriter.writeMatrix(context, "matrix1.txt", matrix1);
        FileWriter.writeMatrix(context, "matrix2.txt", matrix2);

        int[][] res = new int[rows1][cols2];
        for (int i = 0; i < rows1; i++) {
            for (int j = 0; j < cols2; j++) {
                res[i][j] = partitionResults.get(i * cols2 + j);
            }
        }

        FileWriter.writeMatrix(context, "res_matrix.txt", res);

    }


    private class WorkerComparator implements Comparator<Worker> {

        @Override
        public int compare(Worker worker1, Worker worker2) {

            if (Math.abs(worker1.getDeviceStats().getBatteryLevel() - worker2.getDeviceStats().getBatteryLevel()) > Thresholds.BATTERY_LEVEL_DIFFERENCE) {
                return worker1.getDeviceStats().getBatteryLevel() - worker2.getDeviceStats().getBatteryLevel();
            }

            if (worker1.getDeviceStats().isCharging() && worker1.getDeviceStats().isCharging()) {
                return worker1.getDeviceStats().getBatteryLevel() - worker2.getDeviceStats().getBatteryLevel();
            } else if (worker1.getDeviceStats().isCharging()) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public final static class Thresholds {
        /* Worker with a battery level more than that of other worker
           by this threshold amount is considered superior.

           Example: worker1 = 80
                    worker2 = 50
                    worker1 is superior
           Otherwise, we see which device is currently plugged in for charging
         */
        public final static int BATTERY_LEVEL_DIFFERENCE = 20;

        /* Minimum battery level to be accepted as a worker */
        public final static int MINIMUM_BATTERY_LEVEL = 40;
    }

}
