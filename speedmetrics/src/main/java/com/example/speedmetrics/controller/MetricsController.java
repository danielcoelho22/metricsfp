package com.example.speedmetrics.controller;

import com.example.speedmetrics.model.metrics.MetricsInfo;
import com.example.speedmetrics.model.metrics.MetricsResponse;
import com.example.speedmetrics.service.LineSpeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api2")
public class MetricsController {

    private final LineSpeedService lineSpeedService;

    @Autowired
    public MetricsController(LineSpeedService lineSpeedService) {
        this.lineSpeedService = lineSpeedService;
    }

    /**
     * It responds with the metrics of the line specified by lineId in the body, and with code 200 or 404.
     * The metrics are calculated based on the speed measured the past 60 minutes.
     * @param lineId - the id of the production line
     * @return - metrics of a specific line
     */
    @GetMapping("/metrics")
    public ResponseEntity<MetricsResponse> metrics(@RequestParam Long lineId) {
        return lineSpeedService.metrics(lineId);
    }

    /**
     * Return the list of metrics of each known line
     * @return - list of metrics of each known line
     */
    @GetMapping("/allMetrics")
    public List<MetricsInfo> getAllMetrics() {
        return lineSpeedService.getAllMetrics();
    }
}
