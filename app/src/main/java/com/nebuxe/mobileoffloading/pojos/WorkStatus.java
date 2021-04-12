package com.nebuxe.mobileoffloading.pojos;

import java.io.Serializable;

public class WorkStatus implements Serializable {
    private int partitionIndex;
    private String status;
    private int result;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPartitionIndex() {
        return partitionIndex;
    }

    public void setPartitionIndex(int partitionIndex) {
        this.partitionIndex = partitionIndex;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
