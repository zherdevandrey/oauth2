package com.example.postservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
public class PostController {

    @GetMapping("/messages")
    public List<Post> posts() {
        log.info("Get posts request");
        var post = new Post(UUID.randomUUID(), "post name");
        return List.of(post);
    }

    record Post(UUID id, String name) {
    }

}