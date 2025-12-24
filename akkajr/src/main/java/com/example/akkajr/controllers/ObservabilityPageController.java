package com.example.akkajr.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ObservabilityPageController {

    @GetMapping("/observability")
    public String observability() {
        return "observability";
    }
}
