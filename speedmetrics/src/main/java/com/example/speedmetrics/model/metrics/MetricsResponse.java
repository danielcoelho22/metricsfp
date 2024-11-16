package com.example.speedmetrics.model.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricsResponse {
    private float avg; //mean of the speed values of the last 60 minutes
    private float max; //highest speed value in the last 60 minutes
    private float min; //lowest speed value the last 60 minutes
}
