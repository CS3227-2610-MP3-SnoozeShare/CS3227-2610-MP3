package com.snoozeshare.controller.guest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.request.AiSearchRequest;
import com.snoozeshare.api.response.AiSearchResponse;
import com.snoozeshare.config.ConditionalOnDatabase;

/**
 * AI-assisted search for guests.
 */
@RestController
@RequestMapping("/api/guest/ai")
@ConditionalOnDatabase
public class GuestAiSearchController {

    private final GuestAiSearchService aiSearchService;

    GuestAiSearchController(GuestAiSearchService aiSearchService) {
        this.aiSearchService = aiSearchService;
    }

    @PostMapping("/search")
    AiSearchResponse search(@Valid @RequestBody AiSearchRequest request,
                            HttpServletRequest httpRequest) {
        return aiSearchService.search(ResolvedUser.id(httpRequest), request.query());
    }
}
