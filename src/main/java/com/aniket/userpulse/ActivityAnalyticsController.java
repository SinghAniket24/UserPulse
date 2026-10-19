package com.aniket.userpulse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class ActivityAnalyticsController {

    private final ActivityAnalyticsService analyticsService;

    public ActivityAnalyticsController(
            ActivityAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public Map<String, Long> getCounts() {
        return analyticsService.getCounts();
    }
}