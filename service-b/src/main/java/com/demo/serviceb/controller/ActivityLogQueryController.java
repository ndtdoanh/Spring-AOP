package com.demo.serviceb.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.serviceb.entity.ActivityLog;
import com.demo.serviceb.service.ActivityLogQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
public class ActivityLogQueryController {

    private final ActivityLogQueryService activityLogQueryService;

    /**
     * GET /api/activity-logs?page=0&size=20&sort=createdAt,desc
     *
     * Mặc định: 20 records, sort createdAt desc — mới nhất lên đầu.
     * Không dùng findAll() không giới hạn — OOM khi data lớn.
     */
    @GetMapping
    public Page<ActivityLog> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return activityLogQueryService.findAll(pageable);
    }
}