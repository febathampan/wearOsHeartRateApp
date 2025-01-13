package com.appforall.heartratewearapp;

public class HeartBeatRate {
    private String id = "";
    private float heartBeatRate = 0;

    public HeartBeatRate(String id, float heartBeatRate) {
        this.heartBeatRate = heartBeatRate;
        this.id = id;
    }

    public float getHeartBeatRate() {
        return heartBeatRate;
    }

    public void setHeartBeatRate(float heartBeatRate) {
        this.heartBeatRate = heartBeatRate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
