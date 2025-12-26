package com.example.akkajr;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class AkkajrApplication {

    public static void main(String[] args) {
        SpringApplication.run(AkkajrApplication.class, args);
    }

    
}