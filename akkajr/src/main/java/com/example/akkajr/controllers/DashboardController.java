package com.example.akkajr.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    
    @Value("${app.service.name:akkajr}")
    private String currentServiceName;
    
    @Value("${app.remote.services:}")
    private String remoteServicesConfig;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("serviceName", currentServiceName);
        model.addAttribute("remoteServices", remoteServicesConfig);
        return "dashboard";
    }
}