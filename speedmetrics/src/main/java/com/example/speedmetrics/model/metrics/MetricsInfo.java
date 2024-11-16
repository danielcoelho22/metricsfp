package com.example.speedmetrics.model.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricsInfo {
    private long lineId;
    private MetricsResponse measures;
}
