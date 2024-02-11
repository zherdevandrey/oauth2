package com.example.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @GetMapping("/server")
    @ResponseStatus(HttpStatus.OK)
    public String hello() {
        return "Hello";
    }
}