package com.example.library;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@Slf4j
@SpringBootApplication
public class LibraryApplication {

    public static void main(String[] args) {
        log.info("=====Boot app with loggers up and running....");
        SpringApplication.run(LibraryApplication.class, args);
    }

}
