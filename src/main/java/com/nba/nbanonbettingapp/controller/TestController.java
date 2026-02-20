package com.nba.nbanonbettingapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Testing if the backend is working or not
@RestController
public class TestController {
    @GetMapping("/test")
    public String test(){
        return "Backend is running successfully";
    }
}
