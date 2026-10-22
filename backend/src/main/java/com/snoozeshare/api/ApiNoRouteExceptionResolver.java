package com.snoozeshare.api;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.auth.security.ProtectedRequestPath;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Writes the shared JSON error body for unmatched API routes at the servlet resolver boundary. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiNoRouteExceptionResolver implements HandlerExceptionResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiNoRouteExceptionResolver.class);

    private final ObjectMapper objectMapper;

    public ApiNoRouteExceptionResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception exception) {
        if (!(exception instanceof NoHandlerFoundException)
                && !(exception instanceof NoResourceFoundException)) {
            return null;
        }
        String path = ProtectedRequestPath.withinApplication(request);
        if (!path.equals("/api/agent") && !path.startsWith("/api/agent/")) {
            return null;
        }
        String requestId = UUID.randomUUID().toString();
        Object actor = request.getAttribute(CurrentUserIdResolver.ATTRIBUTE);
        String actorId = actor instanceof UUID id ? id.toString() : "unknown";
        LOGGER.warn("API request failed requestId={} status=404 code=NOT_FOUND path={} agentId={}",
                requestId, path, actorId);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            objectMapper.writeValue(response.getOutputStream(),
                    new ApiErrorResponse("NOT_FOUND", "Not found", requestId));
        } catch (IOException exceptionWritingBody) {
            LOGGER.warn("API error response could not be written requestId={}", requestId);
        }
        return new ModelAndView();
    }
}
