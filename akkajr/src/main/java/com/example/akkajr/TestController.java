package com.example.akkajr;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    
    @GetMapping("/")
    public String home() {
        return "Bienvenue ! L'application fonctionne correctement 🚀";
    }
    
    @GetMapping("/test")
    public String test() {
        return "Test OK !";
    }
}