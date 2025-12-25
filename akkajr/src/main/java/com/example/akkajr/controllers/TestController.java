package com.example.akkajr.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
// L'import akka.actor.typed.ActorRef a été supprimé ici

@RestController
public class TestController {
    @GetMapping("/test")
    public String test() {
        return "Controller OK";
    }
}