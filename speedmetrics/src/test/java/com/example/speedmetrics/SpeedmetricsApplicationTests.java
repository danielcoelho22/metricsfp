package com.example.speedmetrics;

import com.example.speedmetrics.model.linespeed.LineSpeedRequest;
import com.example.speedmetrics.model.metrics.MetricsInfo;
import com.example.speedmetrics.model.metrics.MetricsResponse;
import com.example.speedmetrics.service.LineSpeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SpeedmetricsApplicationTests {

    private LineSpeedService lineSpeedService;
    private long currTimeMillis;

    @BeforeEach
    void setUp() {
        currTimeMillis = Instant.now().toEpochMilli();
        lineSpeedService = new LineSpeedService();
    }

    @Test
    void lineSpeedMethod_ShouldReturnCreated_WhenValidLineIdAndRecentTimestamp() {
        LineSpeedRequest request = new LineSpeedRequest(2L, 123, currTimeMillis);

        HttpStatus status = lineSpeedService.lineSpeedMethod(request);

        assertEquals(HttpStatus.CREATED, status);
    }

    @Test
    void lineSpeedMethod_ShouldReturnNotFound_WhenUnknownLineId() {
        LineSpeedRequest request = new LineSpeedRequest(777L, 20, currTimeMillis);

        HttpStatus status = lineSpeedService.lineSpeedMethod(request);

        assertEquals(HttpStatus.NOT_FOUND, status);
    }

    @Test
    void lineSpeedMethod_ShouldReturnNoContent_WhenTimestampIsOlderThan60Minutes() {
        long oldTimestamp = Instant.now().minusSeconds(3601).toEpochMilli();
        LineSpeedRequest request = new LineSpeedRequest(2L, 30, oldTimestamp);

        HttpStatus status = lineSpeedService.lineSpeedMethod(request);

        assertEquals(HttpStatus.NO_CONTENT, status);
    }

    @Test
    void metrics_ShouldReturnMetrics_WhenDataExists() {
        long currentTime = Instant.now().toEpochMilli();
        lineSpeedService.lineSpeedMethod(new LineSpeedRequest(2L, 10, currentTime));
        lineSpeedService.lineSpeedMethod(new LineSpeedRequest(2L, 32, currentTime - 1));

        ResponseEntity<MetricsResponse> response = lineSpeedService.metrics(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MetricsResponse metrics = response.getBody();
        assertNotNull(metrics);
        assertEquals(21.0f, metrics.getAvg());
        assertEquals(32.0f, metrics.getMax());
        assertEquals(10.0f, metrics.getMin());
    }

    @Test
    void metrics_ShouldReturnNoContent_WhenNoDataExists() {
        ResponseEntity<MetricsResponse> response = lineSpeedService.metrics(2L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void metrics_ShouldReturnNotFound_WhenUnknownLineId() {
        ResponseEntity<MetricsResponse> response = lineSpeedService.metrics(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getAllMetrics_ShouldReturnMetricsForAllKnownLines_WhenDataExists() {
        long currentTime = Instant.now().toEpochMilli();
        lineSpeedService.lineSpeedMethod(new LineSpeedRequest(2L, 50.0f, currentTime));
        lineSpeedService.lineSpeedMethod(new LineSpeedRequest(4L, 70.0f, currentTime));

        List<MetricsInfo> allMetrics = lineSpeedService.getAllMetrics();

        assertEquals(2, allMetrics.size());
        MetricsInfo line2Metrics = allMetrics.stream().filter(m -> m.getLineId() == 2L).findFirst().orElse(null);
        MetricsInfo line4Metrics = allMetrics.stream().filter(m -> m.getLineId() == 4L).findFirst().orElse(null);

        assertNotNull(line2Metrics);
        assertEquals(50.0f, line2Metrics.getMeasures().getAvg());

        assertNotNull(line4Metrics);
        assertEquals(70.0f, line4Metrics.getMeasures().getAvg());
    }

    @Test
    void removeOldData_ShouldPruneOldMeasurements() {
        long currentTime = Instant.now().toEpochMilli();
        lineSpeedService.lineSpeedMethod(new LineSpeedRequest(2L, 30.0f, currentTime - 3500 * 1000));
        lineSpeedService.lineSpeedMethod(new LineSpeedRequest(2L, 70.0f, currentTime));

        ResponseEntity<MetricsResponse> response = lineSpeedService.metrics(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MetricsResponse metrics = response.getBody();
        assertNotNull(metrics);
        assertEquals(50.0f, metrics.getAvg());
        assertEquals(70.0f, metrics.getMax());
        assertEquals(30.0f, metrics.getMin());
    }

    @Test
    void lineSpeedMethod_ShouldUpdateMetricsOnMultipleRequests() {
        long currentTime = Instant.now().toEpochMilli();
        lineSpeedService.lineSpeedMethod(new LineSpeedRequest(2L, 40.0f, currentTime));
        lineSpeedService.lineSpeedMethod(new LineSpeedRequest(2L, 60.0f, currentTime + 1000));
        lineSpeedService.lineSpeedMethod(new LineSpeedRequest(2L, 80.0f, currentTime + 2000));

        ResponseEntity<MetricsResponse> response = lineSpeedService.metrics(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MetricsResponse metrics = response.getBody();
        assertNotNull(metrics);
        assertEquals(60.0f, metrics.getAvg());
        assertEquals(80.0f, metrics.getMax());
        assertEquals(40.0f, metrics.getMin());
    }

}
