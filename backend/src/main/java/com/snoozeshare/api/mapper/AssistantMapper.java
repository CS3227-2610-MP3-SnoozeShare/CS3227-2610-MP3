package com.snoozeshare.api.mapper;

import com.snoozeshare.api.response.AssistantResponse;
import com.snoozeshare.service.DisputeDetail.AssistantSnapshot;

/** Converts the response-safe service snapshot, preserving absent assistant rows. */
public final class AssistantMapper {
    private AssistantMapper() {
    }

    public static AssistantResponse from(AssistantSnapshot assistant) {
        return assistant == null ? null : new AssistantResponse(assistant.state(), assistant.rounds(),
                assistant.maxRounds(), assistant.flagged(), assistant.stale(), assistant.draft(),
                assistant.draftUpdatedAt());
    }
}
