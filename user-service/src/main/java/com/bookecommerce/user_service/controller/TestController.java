package com.yourteam.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class TestController {

    @GetMapping("/ping")
    public Map ping() {
        return Map.of("status", "SUCCESS", "message", "Routed via API Gateway!");
    }
}