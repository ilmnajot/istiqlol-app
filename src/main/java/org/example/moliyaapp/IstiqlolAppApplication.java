package org.example.moliyaapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class IstiqlolAppApplication {

    public static void main(String[] args) {

        // Set JVM timezone to Tashkent for Contabo server
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"));
        SpringApplication.run(IstiqlolAppApplication.class, args);


    }

}
