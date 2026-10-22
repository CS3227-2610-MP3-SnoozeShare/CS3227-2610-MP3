package com.snoozeshare.api.controller.agent;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.response.AuditEntryResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.service.AuditFilter;
import com.snoozeshare.service.AuditQueryService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Searches the audit log and lists the action types its filter offers. */
@RestController
@RequestMapping("/api/agent/audit")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
@ConditionalOnDatabase
public class AgentAuditController {

    private final AuditQueryService audit;

    public AgentAuditController(AuditQueryService audit) {
        this.audit = audit;
    }

    @GetMapping
    public List<AuditEntryResponse> search(
            @RequestParam(required = false) @Size(max = 100) String text,
            @RequestParam(required = false) @Size(max = 40) List<AuditAction> actions,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "200") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        Set<AuditAction> chosen = actions == null ? Set.of()
                : actions.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return audit.search(new AuditFilter(text, chosen, from, to), limit, offset).stream()
                .map(AuditEntryResponse::from).toList();
    }

    @GetMapping("/actions")
    public List<String> actions() {
        return Arrays.stream(AuditAction.values()).map(Enum::name).toList();
    }
}
