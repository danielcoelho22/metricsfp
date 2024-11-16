package com.example.speedmetrics.controller;


import org.springframework.web.bind.annotation.*;

@RestController
public class PingController {

    @GetMapping("/ping")
    public String ping() {
        return "Pong";
    }
}
