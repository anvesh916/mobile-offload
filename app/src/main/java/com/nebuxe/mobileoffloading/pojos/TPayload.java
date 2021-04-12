package com.nebuxe.mobileoffloading.pojos;

import java.io.Serializable;

public class TPayload implements Serializable {
    private String tag;
    private Object data;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
