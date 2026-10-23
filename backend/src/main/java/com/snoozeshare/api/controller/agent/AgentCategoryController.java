package com.snoozeshare.api.controller.agent;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.CurrentUserId;
import com.snoozeshare.api.mapper.CategoryMapper;
import com.snoozeshare.api.request.CreateCategoryRequest;
import com.snoozeshare.api.request.SetCategoryActiveRequest;
import com.snoozeshare.api.response.TicketCategoryResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.service.TicketService;

import jakarta.validation.Valid;

/** Adds, renames, toggles and deletes dispute ticket categories. */
@RestController
@RequestMapping("/api/agent/categories")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
@ConditionalOnDatabase
public class AgentCategoryController {

    private final TicketService tickets;

    public AgentCategoryController(TicketService tickets) {
        this.tickets = tickets;
    }

    @GetMapping
    public List<TicketCategoryResponse> list() {
        return CategoryMapper.toResponses(tickets.listAllCategories());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCategoryResponse create(@Valid @RequestBody CreateCategoryRequest request,
                                         @CurrentUserId UUID agentId) {
        return CategoryMapper.toResponse(tickets.createCategory(request.label(), agentId));
    }

    @PutMapping("/{categoryId}")
    public TicketCategoryResponse rename(@PathVariable UUID categoryId,
                                         @Valid @RequestBody CreateCategoryRequest request,
                                         @CurrentUserId UUID agentId) {
        return CategoryMapper.toResponse(tickets.renameCategory(categoryId, request.label(), agentId));
    }

    @PutMapping("/{categoryId}/active")
    public TicketCategoryResponse setActive(@PathVariable UUID categoryId,
                                            @Valid @RequestBody SetCategoryActiveRequest request,
                                            @CurrentUserId UUID agentId) {
        return CategoryMapper.toResponse(tickets.setCategoryActive(categoryId, request.active(), agentId));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID categoryId, @CurrentUserId UUID agentId) {
        tickets.deleteCategory(categoryId, agentId);
    }
}
