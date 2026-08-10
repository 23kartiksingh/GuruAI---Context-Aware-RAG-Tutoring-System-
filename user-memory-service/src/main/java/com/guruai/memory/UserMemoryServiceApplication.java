package com.guruai.memory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class UserMemoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserMemoryServiceApplication.class, args);
    }
}
