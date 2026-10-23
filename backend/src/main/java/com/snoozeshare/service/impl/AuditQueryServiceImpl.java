package com.snoozeshare.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.service.AuditEntry;
import com.snoozeshare.service.AuditFilter;
import com.snoozeshare.service.AuditQueryService;
import com.snoozeshare.service.AuditService;

@Service
@ConditionalOnDatabase
public class AuditQueryServiceImpl implements AuditQueryService {

    private final AuditService audit;

    public AuditQueryServiceImpl(AuditService audit) {
        this.audit = audit;
    }

    @Override
    public List<AuditEntry> search(AuditFilter filter, int limit, int offset) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new IllegalArgumentException("The From date must not be after the To date");
        }
        return audit.search(filter, limit, offset).stream().map(AuditEntry::from).toList();
    }
}
