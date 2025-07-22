package com.ns.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @GetMapping("/fallback/document")
    public Mono<String> documentServiceFallback() {
        return Mono.just("Document Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/fallback/graph")
    public Mono<String> graphServiceFallback() {
        return Mono.just("Graph Service is currently unavailable. Please try again later.");
    }


    @GetMapping("/fallback")
    public Mono<String> defaultFallback() {
        return Mono.just("Service is currently unavailable. Please try again later.");
    }
}