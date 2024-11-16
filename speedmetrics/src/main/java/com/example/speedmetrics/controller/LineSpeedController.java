package com.example.speedmetrics.controller;

import com.example.speedmetrics.model.linespeed.LineSpeedRequest;
import com.example.speedmetrics.service.LineSpeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api1")
public class LineSpeedController {

    private final LineSpeedService lineSpeedService;

    @Autowired
    public LineSpeedController(LineSpeedService lineSpeedService) {
        this.lineSpeedService = lineSpeedService;
    }

    /**
     * Every time a value for the speed of a line is measured, this endpoint will be called
     * Add the specific measures for x lineId
     * @param lineSpeedRequest - object with the measures of a specific production line
     * @return - status code about the process of adding the measures
     */
    @PostMapping("/linespeed")
    public ResponseEntity<Void> lineSpeed(@RequestBody LineSpeedRequest lineSpeedRequest) {
        HttpStatus reponseStatus = lineSpeedService.lineSpeedMethod(lineSpeedRequest);
        return new ResponseEntity<>(reponseStatus);
    }

}
