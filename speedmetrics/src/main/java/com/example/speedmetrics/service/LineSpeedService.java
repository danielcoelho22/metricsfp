package com.example.speedmetrics.service;

import com.example.speedmetrics.model.linespeed.LineSpeedRequest;
import com.example.speedmetrics.model.metrics.MetricsInfo;
import com.example.speedmetrics.model.metrics.MetricsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LineSpeedService {

    private static final long SIXTY_MIN_IN_MS = 60 * 60 * 1000;

    //ConcurrentHashMap with key -> lineId and value -> LinkedList<LineSpeedRequest> with the necessary measures
    //enable efficient addition/removal of data at both ends.
    private final Map<Long, LinkedList<LineSpeedRequest>> data = new ConcurrentHashMap<>();
    private final Set<Long> lineIds = new HashSet<>(Arrays.asList(2L, 4L, 6L, 8L, 10L)); // static line IDs

    // Precomputed metrics
    private final Map<Long, Float> cumulativeSum = new ConcurrentHashMap<>();
    private final Map<Long, Integer> count = new ConcurrentHashMap<>();
    private final Map<Long, Float> minSpeed = new ConcurrentHashMap<>();
    private final Map<Long, Float> maxSpeed = new ConcurrentHashMap<>();


    /**
     * Every time a value for the speed of a line is measured, this endpoint will be called
     * Add the specific measures for x lineId
     * @param item - lineSpeedRequest
     * @return
     */
    public HttpStatus lineSpeedMethod(LineSpeedRequest item) {
        //404 - NOT FOUND
        //404: if line_id is unknown
        if (!lineIds.contains(item.getLineId())) return HttpStatus.NOT_FOUND;

        //204 - NO CONTENT
        //204: if the measurement is older than 60 minutes
        //(epoch in millis in UTC time zone (this is not the current timestamp))
        //timestamp -> millis, 60 min -> (60)minutes * 60 * 1000 (SIXTY_MIN_IN_MS)
        long currTimeMillis = Instant.now().toEpochMilli();
        //if time between current time and item.getTimestamp() > SIXTY_MIN_IN_MS means that the measurement is older than 60 minutes
        if (currTimeMillis - item.getTimestamp() > SIXTY_MIN_IN_MS) return HttpStatus.NO_CONTENT;

        //ALL OK to proceed

        // The computeIfAbsent method checks whether the key (measurement.getLineId()) already exists in the map.
        // If it does not exist, the lambda is executed and the value (new LinkedList<>()) is inserted into the map,associated with the key.
        // If it already exists, the value associated with that key is returned directly (without modifications).
        data.computeIfAbsent(item.getLineId(), x -> new LinkedList<>()).offer(item);

        // update all cumulative metrics and remove the data (mantain last 60min)
        updateCumulativeMetrics(item);
        removeOldData(item.getLineId());

        //201 - CREATED
        //if the operation is successful and none of the other checks apply
        return HttpStatus.CREATED;
    }

    /**
     * Update all cumulative Metrics (cumulativeSum, count, minSpeed, maxSpeed)
     * @param item
     */
    private void updateCumulativeMetrics(LineSpeedRequest item) {
        long lineId = item.getLineId();
        float speed = item.getSpeed();

        cumulativeSum.merge(lineId, speed, Float::sum);
        count.merge(lineId, 1, Integer::sum);

        minSpeed.merge(lineId, speed, Math::min);
        maxSpeed.merge(lineId, speed, Math::max);
    }

    /**
     * Method only removes outdated data.
     * Maintain only a fixed amount of recent data (e.g., the last 60 minutes)
     * This avoids recalculating metrics from scratch, keeping operations O(1).
     * @param lineId
     */
    private void removeOldData(Long lineId) {
        LinkedList<LineSpeedRequest> list = data.get(lineId);
        if (list == null) return;

        long cutoff = Instant.now().toEpochMilli() - SIXTY_MIN_IN_MS;
        while (!list.isEmpty() && list.peek().getTimestamp() < cutoff) {
            LineSpeedRequest old = list.poll();
            updateMetricsOnRemove(old);
        }
    }

    /**
     * Update all precomputed metrics
     * @param item
     */
    private void updateMetricsOnRemove(LineSpeedRequest item) {
        long lineId = item.getLineId();
        float speed = item.getSpeed();

        cumulativeSum.merge(lineId, -speed, Float::sum);
        count.merge(lineId, -1, Integer::sum);

        if (count.get(lineId) > 0) {
            // Recalculate min/max if necessary
            LinkedList<LineSpeedRequest> measures = data.get(lineId);
            if (measures != null) {
                minSpeed.put(lineId, measures.stream().min(Comparator.comparing(LineSpeedRequest::getSpeed)).get().getSpeed());
                maxSpeed.put(lineId, measures.stream().max(Comparator.comparing(LineSpeedRequest::getSpeed)).get().getSpeed());
            }
        } else {
            minSpeed.remove(lineId);
            maxSpeed.remove(lineId);
        }
    }

    /**
     * It responds with the metrics of the line specified by lineId in the body, and with code 200 or 404.
     * The metrics are calculated based on the speed measured the past 60 minutes.
     * @param lineId - the id of the production line
     * @return - metrics of a specific line
     */
    public ResponseEntity<MetricsResponse> metrics(Long lineId) {
        //404 - NOT FOUND
        //404: if lineId is unknown
        if (!lineIds.contains(lineId)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        removeOldData(lineId);

        Integer count = this.count.getOrDefault(lineId, 0);
        if (count == 0) return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

        float avg = cumulativeSum.get(lineId) / count;
        float max = maxSpeed.get(lineId);
        float min = minSpeed.get(lineId);

        return ResponseEntity.ok(new MetricsResponse(avg, max, min));
    }

    /**
     * Return the list of metrics of each known line
     * @return - list of metrics of each known line
     */
    public List<MetricsInfo> getAllMetrics() {
        List<MetricsInfo> metricsInfoList = new ArrayList<>();
        lineIds.forEach(l -> {
            MetricsResponse measure = metrics(l).getBody();
            if (measure != null) metricsInfoList.add(new MetricsInfo(l, measure));
        });

        return metricsInfoList;
    }



}
