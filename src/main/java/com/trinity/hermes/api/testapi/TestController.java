package com.trinity.hermes.api.testapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {
    @GetMapping("/ping")
    public String ping() {
        log.info("request received");
        return "pong"; }
}