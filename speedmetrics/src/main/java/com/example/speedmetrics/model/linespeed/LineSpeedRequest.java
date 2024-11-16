package com.example.speedmetrics.model.linespeed;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LineSpeedRequest {
    private long lineId; //a long, specifying the id of the line.
    private float speed; //a floating-point number, specifying the measured speed of the line.
    private long timestamp; //A long, specifying the time, when the speed was measured, in epoch in millis in UTC time zone (this is not the current timestamp)
}
