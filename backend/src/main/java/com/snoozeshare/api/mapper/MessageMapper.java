package com.snoozeshare.api.mapper;

import java.util.List;

import com.snoozeshare.api.response.MessageResponse;
import com.snoozeshare.domain.model.Message;

/** Maps ticket thread messages to their response records, so controllers never name the entity. */
public final class MessageMapper {

    private MessageMapper() {
    }

    public static List<MessageResponse> toResponses(List<Message> messages) {
        return messages.stream().map(MessageResponse::from).toList();
    }

    public static MessageResponse toResponse(Message message) {
        return MessageResponse.from(message);
    }
}
