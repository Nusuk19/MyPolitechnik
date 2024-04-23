package com.example.mypolytechnik;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.telegram.telegrambots.starter.TelegramBotStarterConfiguration;

@SpringBootApplication
public class MyPolytechnikApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyPolytechnikApplication.class, args);
    }

}