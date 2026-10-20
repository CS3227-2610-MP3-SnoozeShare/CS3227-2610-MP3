package com.snoozeshare.testsupport;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that give CORS something to guard besides the actuator. */
@RestController
public class TestCorsController {

    /**
     * Answers GET requests.
     *
     * @return a fixed body
     */
    @GetMapping("/test/cors")
    public String get() {
        return "ok";
    }

    /**
     * Answers PUT requests.
     *
     * @return a fixed body
     */
    @PutMapping("/test/cors")
    public String put() {
        return "ok";
    }
}
