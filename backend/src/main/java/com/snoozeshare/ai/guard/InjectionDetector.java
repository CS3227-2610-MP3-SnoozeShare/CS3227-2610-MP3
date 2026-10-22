package com.snoozeshare.ai.guard;

import java.util.regex.Pattern;

/**
 * Flags text that looks like a prompt-injection attempt (F4). The flag is advisory: the request still goes out
 * and the patterns are a floor, not a defence.
 */
public final class InjectionDetector {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
    private static final Pattern IGNORE = Pattern.compile(
        "(ignore|disregard|forget).{0,30}(previous|above|prior|all).{0,20}(instruction|prompt|rule)", FLAGS);
    private static final Pattern REVEAL = Pattern.compile("reveal.{0,20}(system|hidden) prompt", FLAGS);
    private static final Pattern BECOME = Pattern.compile("you are now (an? )?(admin|developer|system)", FLAGS);
    private static final Pattern ACT_AS = Pattern.compile("act as (an? )?(admin|developer)", FLAGS);

    private InjectionDetector() { }

    /**
     * Tests one piece of text.
     *
     * @param text normalized text, never null
     * @return true when any flag pattern matches
     */
    public static boolean isSuspicious(String text) {
        return IGNORE.matcher(text).find() || REVEAL.matcher(text).find() || BECOME.matcher(text).find()
            || ACT_AS.matcher(text).find();
    }
}
