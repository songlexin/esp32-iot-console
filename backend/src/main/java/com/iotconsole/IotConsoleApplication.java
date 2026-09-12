package com.iotconsole;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IotConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotConsoleApplication.class, args);
    }
}
