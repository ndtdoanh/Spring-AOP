package com.demo.serviceb.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.serviceb.entity.ActivityLog;
import com.demo.serviceb.repository.ActivityLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * Query service tách riêng khỏi command service — CQRS nhẹ.
 * Mọi query đều readOnly = true để tối ưu connection pool và tránh dirty check.
 */
@Service
@RequiredArgsConstructor
public class ActivityLogQueryService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional(readOnly = true)
    public Page<ActivityLog> findAll(Pageable pageable) {
        return activityLogRepository.findAll(pageable);
    }
}