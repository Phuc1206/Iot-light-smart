package com.example.myapplication;

public class History {
    private String status;
    private String time;

    public History(String status, String time) {
        this.status = status;
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public String getTime() {
        return time;
    }
}

