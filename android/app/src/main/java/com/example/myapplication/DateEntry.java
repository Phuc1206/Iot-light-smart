package com.example.myapplication;

import com.github.mikephil.charting.data.Entry;

import java.time.LocalDate;

public class DateEntry extends Entry {
    private LocalDate date;

    public DateEntry(float x, float y, LocalDate date) {
        super(x, y);
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }
}
