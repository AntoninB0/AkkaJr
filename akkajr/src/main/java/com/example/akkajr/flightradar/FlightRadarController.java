package com.example.akkajr.flightradar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FlightRadarController {
    
    @Value("${app.service.name:service1}")
    private String currentServiceName;
    
    @Value("${app.remote.services:}")
    private String remoteServicesConfig;
    
    @GetMapping("/flightradar")
    public String flightRadar(Model model) {
        model.addAttribute("serviceName", currentServiceName);
        model.addAttribute("remoteServices", remoteServicesConfig);
        return "flightradar";
    }
}