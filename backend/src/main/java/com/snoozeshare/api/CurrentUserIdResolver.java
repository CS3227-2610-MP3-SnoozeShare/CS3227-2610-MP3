package com.snoozeshare.api;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves {@link CurrentUserId} only from the role filter's request attribute. */
public class CurrentUserIdResolver implements HandlerMethodArgumentResolver {

    public static final String ATTRIBUTE = CurrentUserIdResolver.class.getName() + ".actorId";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Object actorId = request == null ? null : request.getAttribute(ATTRIBUTE);
        if (actorId instanceof UUID id) {
            return id;
        }
        throw new MissingActorException();
    }
}
