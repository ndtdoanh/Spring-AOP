package com.demo.serviceb.controller;

import com.demo.serviceb.entity.ActivityLog;
import com.demo.serviceb.repository.ActivityLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
public class ActivityLogQueryController {

    private final ActivityLogRepository activityLogRepository;

    @GetMapping
    public List<ActivityLog> list() {
        return activityLogRepository.findAll();
    }
}
